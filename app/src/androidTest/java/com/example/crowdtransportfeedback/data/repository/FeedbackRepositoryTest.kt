package com.example.crowdtransportfeedback.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.crowdtransportfeedback.data.local.AppDatabase
import com.example.crowdtransportfeedback.data.local.FeedbackEntity
import com.example.crowdtransportfeedback.data.local.SyncState
import com.example.crowdtransportfeedback.data.remote.FeedbackApi
import com.example.crowdtransportfeedback.data.remote.FeedbackDto
import com.example.crowdtransportfeedback.data.remote.toEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class FeedbackRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var api: RecordingFeedbackApi
    private lateinit var repository: FeedbackRepository
    private var scheduled = 0
    private var currentUserId: String? = USER_A

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        api = RecordingFeedbackApi()
        scheduled = 0
        currentUserId = USER_A
        repository = FeedbackRepository(
            dao = database.feedbackDao(),
            api = api,
            scheduleSync = { scheduled++ },
            currentUserId = { currentUserId }
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun uploadUsesDistributedIdAndMarksLocalRowSynchronized() = runBlocking {
        val localId = repository.addFeedbackAndUpload(feedback("feedback-uuid"))
        val stored = repository.getById(localId).first()

        assertEquals("feedback-uuid", api.added.single().id)
        assertNotEquals(localId.toString(), api.added.single().id)
        assertEquals(USER_A, api.added.single().createdByUserId)
        assertEquals(SyncState.SYNCED, stored?.syncState)
        assertEquals(USER_A, stored?.createdByUserId)
    }

    @Test
    fun pendingCreateRetriesAfterNetworkReturns() = runBlocking {
        api.networkAvailable = false
        val localId = repository.addFeedbackAndUpload(feedback("pending-id"))

        assertEquals(SyncState.PENDING_CREATE, repository.getById(localId).first()?.syncState)
        assertEquals(1, scheduled)

        api.networkAvailable = true
        assertFalse(repository.synchronize().transientFailure)
        assertEquals(SyncState.SYNCED, repository.getById(localId).first()?.syncState)
        assertEquals(1, api.added.size)
    }

    @Test
    fun retryIsIdempotentWhenServerAlreadyHasDistributedId() = runBlocking {
        api.networkAvailable = false
        val localId = repository.addFeedbackAndUpload(feedback("already-created"))
        api.remote += feedbackDto("already-created", "remote copy", USER_A)
        api.networkAvailable = true

        repository.synchronize()

        assertEquals(0, api.added.size)
        assertEquals(SyncState.SYNCED, repository.getById(localId).first()?.syncState)
    }

    @Test
    fun uncertainPostIsConfirmedByDistributedId() = runBlocking {
        api.failNextAddAfterPersist = true

        val localId = repository.addFeedbackAndUpload(feedback("uncertain-post"))

        assertEquals(SyncState.SYNCED, repository.getById(localId).first()?.syncState)
        assertEquals(1, api.remote.count { it.id == "uncertain-post" })
        assertEquals(0, scheduled)
    }

    @Test
    fun offlineDeleteLeavesHiddenTombstoneAndSchedulesRetry() = runBlocking {
        val localId = database.feedbackDao().insert(feedback("delete-id", SyncState.SYNCED))
        api.networkAvailable = false

        repository.deleteFeedbackAdmin(localId)

        assertTrue(repository.getAllFeedback().first().isEmpty())
        assertEquals(SyncState.PENDING_DELETE, database.feedbackDao().getByLocalIdOnce(localId)?.syncState)
        assertEquals(1, scheduled)
        assertEquals(0, api.deleteCalls)
    }

    @Test
    fun deleteRetryPermanentlyRemovesTombstone() = runBlocking {
        val localId = database.feedbackDao().insert(feedback("delete-retry", SyncState.SYNCED))
        api.remote += feedbackDto("delete-retry", "remote", USER_A)
        api.networkAvailable = false
        repository.deleteFeedbackAdmin(localId)

        assertEquals(0, api.deleteCalls)

        api.networkAvailable = true
        repository.synchronize()

        assertNull(database.feedbackDao().getByLocalIdOnce(localId))
        assertEquals(listOf("delete-retry"), api.deleted)
        assertEquals(1, api.deleteCalls)
    }

    @Test
    fun delete404IsSuccessfulAndRemovesTombstone() = runBlocking {
        val localId = database.feedbackDao().insert(feedback("missing", SyncState.SYNCED))

        repository.deleteFeedbackAdmin(localId)

        assertEquals(SyncState.PENDING_DELETE, database.feedbackDao().getByLocalIdOnce(localId)?.syncState)
        assertEquals(0, api.deleteCalls)

        repository.synchronize()

        assertNull(database.feedbackDao().getByLocalIdOnce(localId))
        assertEquals(1, api.deleteCalls)
    }

    @Test
    fun repeatedSynchronizationDoesNotPostOrInsertDuplicates() = runBlocking {
        database.feedbackDao().insert(feedback("repeat"))

        repository.synchronize()
        repository.synchronize()

        assertEquals(1, api.added.size)
        assertEquals(1, repository.getAllFeedback().first().count { it.feedbackId == "repeat" })
    }

    @Test
    fun concurrentSynchronizationAcrossRepositoriesCreatesRemoteItemOnce() = runBlocking {
        val localId = database.feedbackDao().insert(feedback("concurrent-id"))
        api.getByIdDelayMillis = 100
        val secondRepository = FeedbackRepository(
            dao = database.feedbackDao(),
            api = api,
            currentUserId = { currentUserId }
        )

        val results = coroutineScope {
            listOf(
                async { repository.synchronize() },
                async { secondRepository.synchronize() }
            ).awaitAll()
        }

        assertTrue(results.none { it.transientFailure })
        assertEquals(1, api.added.count { it.id == "concurrent-id" })
        assertEquals(1, api.remote.count { it.id == "concurrent-id" })
        assertEquals(
            SyncState.SYNCED,
            database.feedbackDao().getByLocalIdOnce(localId)?.syncState
        )
    }

    @Test
    fun repositoryReconciliationPreservesPendingLocalVersion() = runBlocking {
        val localId = database.feedbackDao().insert(feedback("same-id", comment = "pending local"))
        api.remote = listOf(feedbackDto("same-id", "remote copy", USER_A))

        // Test reconciliation directly because a full pass intentionally uploads pending creates first.
        database.feedbackDao().reconcileRemote(api.getAll().map { it.toEntity() })

        val stored = repository.getById(localId).first()
        assertEquals("pending local", stored?.comment)
        assertEquals(SyncState.PENDING_CREATE, stored?.syncState)
        assertEquals(USER_A, stored?.createdByUserId)
    }

    @Test
    fun pendingCreateNeverUploadsUnderAnotherAccountAndUploadsWhenCreatorReturns() = runBlocking {
        val localId = database.feedbackDao().insert(
            feedback("cross-account", createdByUserId = USER_A)
        )

        currentUserId = USER_B
        repository.synchronize()

        val whileOtherUser = database.feedbackDao().getByLocalIdOnce(localId)
        assertEquals(0, api.added.size)
        assertEquals(SyncState.PENDING_CREATE, whileOtherUser?.syncState)
        assertEquals(USER_A, whileOtherUser?.createdByUserId)

        currentUserId = USER_A
        repository.synchronize()

        val afterCreatorReturns = database.feedbackDao().getByLocalIdOnce(localId)
        assertEquals(1, api.added.count { it.id == "cross-account" })
        assertEquals(SyncState.SYNCED, afterCreatorReturns?.syncState)
        assertEquals(USER_A, afterCreatorReturns?.createdByUserId)
    }

    @Test
    fun ownerlessLegacyPendingCreateIsNotUploaded() = runBlocking {
        val localId = database.feedbackDao().insert(
            feedback("legacy-ownerless", createdByUserId = null)
        )

        repository.synchronize()

        assertEquals(0, api.added.size)
        assertEquals(SyncState.PENDING_CREATE, database.feedbackDao().getByLocalIdOnce(localId)?.syncState)
        assertNull(database.feedbackDao().getByLocalIdOnce(localId)?.createdByUserId)
    }

    private fun feedback(
        feedbackId: String,
        syncState: SyncState = SyncState.PENDING_CREATE,
        comment: String = "Comment",
        createdByUserId: String? = USER_A
    ) = FeedbackEntity(
        feedbackId = feedbackId,
        score = 4,
        comment = comment,
        latitude = 44.4268,
        longitude = 26.1025,
        line = "41",
        createdAt = 1_700_000_000_000,
        syncState = syncState,
        createdByUserId = createdByUserId
    )

    private fun feedbackDto(
        feedbackId: String,
        comment: String,
        createdByUserId: String
    ) = FeedbackDto(
        id = feedbackId,
        score = 3,
        comment = comment,
        line = "41",
        createdAt = 1_700_000_000_100,
        latitude = 44.4268,
        longitude = 26.1025,
        createdByUserId = createdByUserId
    )

    companion object {
        private const val USER_A = "11111111-1111-1111-1111-111111111111"
        private const val USER_B = "22222222-2222-2222-2222-222222222222"
    }
}

private class RecordingFeedbackApi : FeedbackApi {
    var networkAvailable = true
    var getByIdDelayMillis = 0L
    var failNextAddAfterPersist = false
    var remote: List<FeedbackDto> = emptyList()
    val added = mutableListOf<FeedbackDto>()
    val deleted = mutableListOf<String>()
    var deleteCalls = 0

    override suspend fun getAll(): List<FeedbackDto> {
        requireNetwork()
        return remote
    }

    override suspend fun getById(id: String): Response<FeedbackDto> {
        requireNetwork()
        delay(getByIdDelayMillis)
        val found = remote.firstOrNull { it.id == id }
        return found?.let { Response.success(it) }
            ?: Response.error(404, "not found".toResponseBody())
    }

    override suspend fun add(item: FeedbackDto): FeedbackDto {
        requireNetwork()
        added += item
        remote += item
        if (failNextAddAfterPersist) {
            failNextAddAfterPersist = false
            throw IOException("response lost after persistence")
        }
        return item
    }

    override suspend fun delete(id: String): Response<Unit> {
        deleteCalls++
        requireNetwork()
        if (remote.none { it.id == id }) {
            return Response.error(404, "not found".toResponseBody())
        }
        remote = remote.filterNot { it.id == id }
        deleted += id
        return Response.success(Unit)
    }

    private fun requireNetwork() {
        if (!networkAvailable) throw IOException("offline")
    }
}

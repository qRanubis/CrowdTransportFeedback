package com.example.crowdtransportfeedback.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.crowdtransportfeedback.data.local.AppDatabase
import com.example.crowdtransportfeedback.data.local.FeedbackEntity
import com.example.crowdtransportfeedback.data.local.SyncState
import com.example.crowdtransportfeedback.data.remote.FeedbackApi
import com.example.crowdtransportfeedback.data.remote.FeedbackDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedbackRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var api: RecordingFeedbackApi
    private lateinit var repository: FeedbackRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        api = RecordingFeedbackApi()
        repository = FeedbackRepository(database.feedbackDao(), api)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun uploadUsesDistributedIdAndMarksLocalRowSynchronized() = runBlocking {
        val item = feedback(feedbackId = "feedback-uuid")

        val localId = repository.addFeedbackAndUpload(item)
        val stored = repository.getById(localId).first()

        assertEquals("feedback-uuid", api.added.single().id)
        assertNotEquals(localId.toString(), api.added.single().id)
        assertEquals(SyncState.SYNCED, stored?.syncState)
    }

    @Test
    fun repositoryReconciliationPreservesPendingLocalVersion() = runBlocking {
        val localId = database.feedbackDao().insert(
            feedback(feedbackId = "same-distributed-id", comment = "pending local")
        )
        api.remote = listOf(
            feedbackDto(feedbackId = "same-distributed-id", comment = "remote copy")
        )

        repository.syncFromRemoteFull()

        val stored = repository.getById(localId).first()
        assertEquals("pending local", stored?.comment)
        assertEquals(SyncState.PENDING_CREATE, stored?.syncState)
    }

    private fun feedback(
        feedbackId: String,
        comment: String = "Comment"
    ) = FeedbackEntity(
        feedbackId = feedbackId,
        score = 4,
        comment = comment,
        latitude = 44.4268,
        longitude = 26.1025,
        line = "41",
        createdAt = 1_700_000_000_000,
        syncState = SyncState.PENDING_CREATE
    )

    private fun feedbackDto(feedbackId: String, comment: String) = FeedbackDto(
        id = feedbackId,
        score = 3,
        comment = comment,
        line = "41",
        createdAt = 1_700_000_000_100,
        latitude = 44.4268,
        longitude = 26.1025
    )
}

private class RecordingFeedbackApi : FeedbackApi {
    var remote: List<FeedbackDto> = emptyList()
    val added = mutableListOf<FeedbackDto>()

    override suspend fun getAll(): List<FeedbackDto> = remote

    override suspend fun add(item: FeedbackDto): FeedbackDto {
        added += item
        return item
    }

    override suspend fun delete(id: String) = Unit
}

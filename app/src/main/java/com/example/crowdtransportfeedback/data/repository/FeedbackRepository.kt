package com.example.crowdtransportfeedback.data.repository

import com.example.crowdtransportfeedback.data.local.FeedbackDao
import com.example.crowdtransportfeedback.data.local.FeedbackEntity
import com.example.crowdtransportfeedback.data.local.SyncState
import com.example.crowdtransportfeedback.data.remote.FeedbackApi
import com.example.crowdtransportfeedback.data.remote.toDto
import com.example.crowdtransportfeedback.data.remote.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.io.IOException

/** The result consumed by WorkManager; only transient failures should cause backoff/retry. */
data class SynchronizationResult(val transientFailure: Boolean)

/** Serializes every full sync pass, including passes started by different repository instances. */
private val synchronizationMutex = Mutex()

class FeedbackRepository(
    private val dao: FeedbackDao,
    private val api: FeedbackApi,
    private val scheduleSync: () -> Unit = {}
) {
    fun getAllFeedback(): Flow<List<FeedbackEntity>> = dao.getAll()

    suspend fun addFeedback(item: FeedbackEntity): Long = dao.insert(item)

    fun getById(localId: Long) = dao.getByLocalId(localId)

    /**
     * Performs the complete deterministic sync pass: deletes, creates, then download/reconcile.
     * Pending Room rows are protected by the DAO's reconciliation transaction.
     */
    suspend fun synchronize(): SynchronizationResult = synchronizationMutex.withLock {
        var transientFailure = false

        dao.getPendingByState(SyncState.PENDING_DELETE).forEach { item ->
            if (processDelete(item) == Attempt.TRANSIENT_FAILURE) transientFailure = true
        }
        dao.getPendingByState(SyncState.PENDING_CREATE).forEach { item ->
            if (processCreate(item) == Attempt.TRANSIENT_FAILURE) transientFailure = true
        }

        try {
            val remoteList = api.getAll()
            dao.reconcileRemote(remoteList.map { it.toEntity() })
        } catch (error: Exception) {
            when {
                error.isTransient() -> transientFailure = true
                error is HttpException -> Unit // A non-transient HTTP response should not back off/retry.
                else -> throw error
            }
        }

        if (transientFailure) scheduleSync()
        SynchronizationResult(transientFailure)
    }

    /** Kept as a compatibility entry point; it now runs a complete two-way synchronization. */
    suspend fun syncFromRemoteFull() {
        synchronize()
    }

    suspend fun deleteFeedbackAdmin(localId: Long) {
        val item = dao.getByLocalIdOnce(localId) ?: return
        dao.setSyncState(localId, SyncState.PENDING_DELETE)
        val tombstone = item.copy(localId = localId, syncState = SyncState.PENDING_DELETE)
        if (processDelete(tombstone) != Attempt.SUCCESS) scheduleSync()
    }

    suspend fun addFeedbackAndUpload(item: FeedbackEntity): Long {
        // Local persistence always happens before any network operation.
        val localId = dao.insert(item.copy(syncState = SyncState.PENDING_CREATE))
        val pending = item.copy(localId = localId, syncState = SyncState.PENDING_CREATE)
        if (processCreate(pending) != Attempt.SUCCESS) scheduleSync()
        return localId
    }

    private suspend fun processCreate(item: FeedbackEntity): Attempt = try {
        val existing = api.getById(item.feedbackId)
        when {
            existing.isSuccessful -> {
                if (existing.body()?.id == item.feedbackId) {
                    dao.setSyncState(item.localId, SyncState.SYNCED)
                    Attempt.SUCCESS
                } else {
                    Attempt.PERMANENT_FAILURE
                }
            }
            existing.code() == 404 -> {
                try {
                    api.add(item.toDto())
                    dao.setSyncState(item.localId, SyncState.SYNCED)
                    Attempt.SUCCESS
                } catch (postError: Exception) {
                    if (postError.isTransient()) confirmCreateAfterUncertainPost(item) else throw postError
                }
            }
            existing.code().isTransientHttpCode() -> Attempt.TRANSIENT_FAILURE
            else -> Attempt.PERMANENT_FAILURE
        }
    } catch (error: Exception) {
        when {
            error.isTransient() -> Attempt.TRANSIENT_FAILURE
            error is HttpException -> Attempt.PERMANENT_FAILURE
            else -> throw error
        }
    }

    /** A timed-out/5xx POST may have persisted remotely, so confirm by distributed ID before retrying. */
    private suspend fun confirmCreateAfterUncertainPost(item: FeedbackEntity): Attempt = try {
        val confirmation = api.getById(item.feedbackId)
        if (confirmation.isSuccessful && confirmation.body()?.id == item.feedbackId) {
            dao.setSyncState(item.localId, SyncState.SYNCED)
            Attempt.SUCCESS
        } else {
            Attempt.TRANSIENT_FAILURE
        }
    } catch (error: Exception) {
        when {
            error.isTransient() -> Attempt.TRANSIENT_FAILURE
            error is HttpException -> Attempt.PERMANENT_FAILURE
            else -> throw error
        }
    }

    private suspend fun processDelete(item: FeedbackEntity): Attempt = try {
        val response = api.delete(item.feedbackId)
        when {
            response.isSuccessful || response.code() == 404 -> {
                dao.deleteByLocalId(item.localId)
                Attempt.SUCCESS
            }
            response.code().isTransientHttpCode() -> Attempt.TRANSIENT_FAILURE
            else -> Attempt.PERMANENT_FAILURE
        }
    } catch (error: Exception) {
        when {
            error.isTransient() -> Attempt.TRANSIENT_FAILURE
            error is HttpException -> Attempt.PERMANENT_FAILURE
            else -> throw error
        }
    }

    private enum class Attempt { SUCCESS, TRANSIENT_FAILURE, PERMANENT_FAILURE }
}

private fun Exception.isTransient(): Boolean =
    this is IOException || (this is HttpException && code().isTransientHttpCode())

private fun Int.isTransientHttpCode(): Boolean = this == 408 || this == 429 || this in 500..599

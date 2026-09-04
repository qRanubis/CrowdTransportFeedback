package com.example.crowdtransportfeedback.data.repository

import com.example.crowdtransportfeedback.auth.UserRole
import com.example.crowdtransportfeedback.data.local.FeedbackDao
import com.example.crowdtransportfeedback.data.local.FeedbackEntity
import com.example.crowdtransportfeedback.data.local.SyncState
import com.example.crowdtransportfeedback.data.remote.FeedbackApi
import com.example.crowdtransportfeedback.data.remote.toDto
import com.example.crowdtransportfeedback.data.remote.toEntity
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

data class SynchronizationResult(val transientFailure: Boolean)

private val synchronizationMutex = Mutex()

class FeedbackRepository(
    private val dao: FeedbackDao,
    private val api: FeedbackApi,
    private val scheduleSync: () -> Unit = {},
    private val currentUserId: () -> String? = { null },
    private val currentUsername: () -> String? = { null },
    private val currentUserRole: () -> UserRole? = { null },
    private val temporaryAuthFailure: () -> Boolean = { false }
) {
    fun getAllFeedback(): Flow<List<FeedbackEntity>> = dao.getAll()

    suspend fun addFeedback(item: FeedbackEntity): Long {
        val creator = requireAuthenticatedCreator()
        val id = dao.insert(
            item.copy(
                syncState = SyncState.PENDING_CREATE,
                createdByUserId = creator,
                createdByUsername = currentUsername()?.takeIf { it.isNotBlank() }
            )
        )
        scheduleSync()
        return id
    }

    fun getById(localId: Long) = dao.getByLocalId(localId)

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
                error.isTransient() || error.isTemporaryAuthenticationFailure() -> transientFailure = true
                error is HttpException -> Unit
                else -> throw error
            }
        }

        if (transientFailure) scheduleSync()
        SynchronizationResult(transientFailure)
    }

    suspend fun syncFromRemoteFull() {
        synchronize()
    }

    suspend fun deleteFeedback(localId: Long) {
        val item = dao.getByLocalIdOnce(localId) ?: return
        val activeUser = currentUserId()
            ?: throw IllegalStateException("An authenticated user is required to delete feedback")
        val canDelete = currentUserRole() == UserRole.ADMIN || item.createdByUserId == activeUser

        if (!canDelete) {
            throw SecurityException("Only the author or an administrator can delete feedback")
        }

        if (item.syncState == SyncState.PENDING_CREATE) {
            dao.deleteByLocalId(localId)
            return
        }

        dao.setSyncState(localId, SyncState.PENDING_DELETE)
        scheduleSync()
    }

    suspend fun deleteFeedbackAdmin(localId: Long) {
        deleteFeedback(localId)
    }

    suspend fun addFeedbackAndUpload(item: FeedbackEntity): Long {
        val creator = requireAuthenticatedCreator()
        val username = currentUsername()?.takeIf { it.isNotBlank() }
        val localId = dao.insert(
            item.copy(
                syncState = SyncState.PENDING_CREATE,
                createdByUserId = creator,
                createdByUsername = username
            )
        )
        val pending = item.copy(
            localId = localId,
            syncState = SyncState.PENDING_CREATE,
            createdByUserId = creator,
            createdByUsername = username
        )
        if (processCreate(pending) != Attempt.SUCCESS) scheduleSync()
        return localId
    }

    private suspend fun processCreate(item: FeedbackEntity): Attempt {
        val creator = item.createdByUserId ?: return Attempt.BLOCKED
        val activeUser = currentUserId() ?: return Attempt.BLOCKED
        if (creator != activeUser) return Attempt.BLOCKED

        return try {
            val existing = api.getById(item.feedbackId)
            when {
                existing.isSuccessful -> {
                    val body = existing.body()
                    if (body?.id == item.feedbackId && body.createdByUserId == creator) {
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
                error.isTransient() || error.isTemporaryAuthenticationFailure() -> Attempt.TRANSIENT_FAILURE
                error is HttpException -> Attempt.PERMANENT_FAILURE
                else -> throw error
            }
        }
    }

    private suspend fun confirmCreateAfterUncertainPost(item: FeedbackEntity): Attempt = try {
        val confirmation = api.getById(item.feedbackId)
        val body = confirmation.body()
        if (
            confirmation.isSuccessful &&
            body?.id == item.feedbackId &&
            body.createdByUserId == item.createdByUserId
        ) {
            dao.setSyncState(item.localId, SyncState.SYNCED)
            Attempt.SUCCESS
        } else {
            Attempt.TRANSIENT_FAILURE
        }
    } catch (error: Exception) {
        when {
            error.isTransient() || error.isTemporaryAuthenticationFailure() -> Attempt.TRANSIENT_FAILURE
            error is HttpException -> Attempt.PERMANENT_FAILURE
            else -> throw error
        }
    }

    private suspend fun processDelete(item: FeedbackEntity): Attempt {
        val activeUser = currentUserId() ?: return Attempt.BLOCKED
        val authorized = currentUserRole() == UserRole.ADMIN || item.createdByUserId == activeUser
        if (!authorized) return Attempt.BLOCKED

        return try {
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
                error.isTransient() || error.isTemporaryAuthenticationFailure() -> Attempt.TRANSIENT_FAILURE
                error is HttpException -> Attempt.PERMANENT_FAILURE
                else -> throw error
            }
        }
    }

    private fun requireAuthenticatedCreator(): String =
        currentUserId()?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("An authenticated user is required to create feedback")

    private enum class Attempt { SUCCESS, TRANSIENT_FAILURE, PERMANENT_FAILURE, BLOCKED }

    private fun Exception.isTemporaryAuthenticationFailure(): Boolean =
        this is HttpException && code() == 401 && temporaryAuthFailure()
}

private fun Exception.isTransient(): Boolean =
    this is IOException || (this is HttpException && code().isTransientHttpCode())

private fun Int.isTransientHttpCode(): Boolean = this == 408 || this == 429 || this in 500..599

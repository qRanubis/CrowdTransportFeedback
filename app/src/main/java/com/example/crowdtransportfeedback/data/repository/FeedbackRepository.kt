package com.example.crowdtransportfeedback.data.repository

import com.example.crowdtransportfeedback.data.local.FeedbackDao
import com.example.crowdtransportfeedback.data.local.FeedbackEntity
import com.example.crowdtransportfeedback.data.local.SyncState
import com.example.crowdtransportfeedback.data.remote.FeedbackApi
import com.example.crowdtransportfeedback.data.remote.toDto
import com.example.crowdtransportfeedback.data.remote.toEntity
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException

class FeedbackRepository(
    private val dao: FeedbackDao,
    private val api: FeedbackApi
) {

    fun getAllFeedback(): Flow<List<FeedbackEntity>> {
        return dao.getAll()
    }

    suspend fun addFeedback(item: FeedbackEntity): Long {
        return dao.insert(item)
    }

    fun getById(localId: Long) = dao.getByLocalId(localId)

    suspend fun syncFromRemoteFull() {
        val remoteList = api.getAll()
        dao.reconcileRemote(remoteList.map { it.toEntity() })
    }

    suspend fun deleteFeedbackAdmin(localId: Long) {
        val item = dao.getByLocalIdOnce(localId) ?: return
        try {
            api.delete(item.feedbackId)
        } catch (e: HttpException) {
            if (e.code() != 404) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // sterge local
        dao.deleteByLocalId(localId)
    }

    suspend fun addFeedbackAndUpload(item: FeedbackEntity): Long {
        // 1) insert local (offline-first)
        val localId = dao.insert(item)

        try {
            api.add(item.toDto())

            // The Room-only key is used only to update the local row.
            dao.setSyncState(localId, SyncState.SYNCED)
        } catch (e: Exception) {
            // Server down: the row remains PENDING_CREATE for a later retry.
            e.printStackTrace()
        }

        return localId
    }
}

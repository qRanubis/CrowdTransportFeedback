package com.example.crowdtransportfeedback.data.repository
import com.example.crowdtransportfeedback.data.local.FeedbackDao
import com.example.crowdtransportfeedback.data.local.FeedbackEntity
import kotlinx.coroutines.flow.Flow
import com.example.crowdtransportfeedback.data.remote.RetrofitClient
import com.example.crowdtransportfeedback.data.remote.FeedbackDto
import retrofit2.HttpException

class FeedbackRepository(
    private val dao: FeedbackDao
) {

    fun getAllFeedback(): Flow<List<FeedbackEntity>> {
        return dao.getAll()
    }

    suspend fun addFeedback(item: FeedbackEntity): Long {
        return dao.insert(item)
    }

    fun getById(id: Long) = dao.getById(id)

    suspend fun syncFromRemoteFull() {
        val remoteList = RetrofitClient.api.getAll()

        // ids existente pe server
        val serverIds = remoteList.mapNotNull { it.id?.toLongOrNull() }

        // replace ce vine din server
        val entities = remoteList.mapNotNull { dto ->
            val idLong = dto.id?.toLongOrNull() ?: return@mapNotNull null

            FeedbackEntity(
                id = idLong,
                score = dto.score,
                comment = dto.comment,
                latitude = dto.latitude,
                longitude = dto.longitude,
                line = dto.line,
                createdAt = dto.createdAt,
                synced = true
            )
        }
        dao.insertAll(entities)

        // 2) stergere
        if (serverIds.isEmpty()) {
            dao.deleteAllSynced()
        } else {
            dao.deleteSyncedNotInServer(serverIds)
        }
    }


    suspend fun deleteFeedbackAdmin(id: Long) {
        try {
            RetrofitClient.api.delete(id)
        } catch (e: HttpException) {
            if (e.code() != 404) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // sterge local
        dao.deleteById(id)
    }


    suspend fun addFeedbackAndUpload(item: FeedbackEntity): Long {
        // 1) insert local (offline-first)
        val localId = dao.insert(item)

        try {
            val dto = FeedbackDto(
                id = localId.toString(),   // ✅ STRING
                score = item.score,
                comment = item.comment,
                line = item.line,
                createdAt = item.createdAt,
                latitude = item.latitude,
                longitude = item.longitude
            )

            // 2) POST (nu ne intereseaza raspunsul)
            RetrofitClient.api.add(dto)

            // 3) marcam synced folosind ID-ul LOCAL
            dao.setSynced(localId, true)

        } catch (e: Exception) {
            // server down → ramane synced=false
            e.printStackTrace()
        }

        return localId
    }






}

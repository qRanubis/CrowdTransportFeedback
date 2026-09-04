package com.example.crowdtransportfeedback.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedbackDao {

    @Query("SELECT * FROM feedback WHERE syncState != 'PENDING_DELETE' ORDER BY createdAt DESC")
    fun getAll(): Flow<List<FeedbackEntity>>

    @Query("SELECT * FROM feedback WHERE localId = :localId")
    fun getByLocalId(localId: Long): Flow<FeedbackEntity?>

    @Query("SELECT * FROM feedback WHERE localId = :localId")
    suspend fun getByLocalIdOnce(localId: Long): FeedbackEntity?

    @Query("SELECT * FROM feedback WHERE syncState != 'SYNCED' ORDER BY createdAt ASC")
    suspend fun getPending(): List<FeedbackEntity>

    @Query("SELECT * FROM feedback WHERE syncState = :syncState ORDER BY createdAt ASC")
    suspend fun getPendingByState(syncState: SyncState): List<FeedbackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FeedbackEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FeedbackEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRemote(item: FeedbackEntity): Long

    @Query("UPDATE feedback SET syncState = :syncState WHERE localId = :localId")
    suspend fun setSyncState(localId: Long, syncState: SyncState)

    @Query(
        """
        UPDATE feedback SET
            score = :score,
            comment = :comment,
            latitude = :latitude,
            longitude = :longitude,
            line = :line,
            createdAt = :createdAt,
            transportType = :transportType,
            crowdingScore = :crowdingScore,
            cleanlinessScore = :cleanlinessScore,
            punctualityScore = :punctualityScore,
            createdByUserId = :createdByUserId
        WHERE feedbackId = :feedbackId AND syncState = 'SYNCED'
        """
    )
    suspend fun updateSyncedFromRemote(
        feedbackId: String,
        score: Int,
        comment: String,
        latitude: Double?,
        longitude: Double?,
        line: String?,
        createdAt: Long,
        transportType: com.example.crowdtransportfeedback.domain.TransportType?,
        crowdingScore: Int?,
        cleanlinessScore: Int?,
        punctualityScore: Int?,
        createdByUserId: String?
    ): Int

    @Query("DELETE FROM feedback WHERE syncState = 'SYNCED'")
    suspend fun deleteAllSynced()

    @Query("DELETE FROM feedback")
    suspend fun deleteAll()

    @Query("DELETE FROM feedback WHERE localId = :localId")
    suspend fun deleteByLocalId(localId: Long)

    @Query("DELETE FROM feedback WHERE syncState = 'SYNCED' AND feedbackId NOT IN (:serverIds)")
    suspend fun deleteSyncedNotInServer(serverIds: List<String>)

    @Transaction
    suspend fun reconcileRemote(items: List<FeedbackEntity>) {
        items.forEach { item ->
            val updated = updateSyncedFromRemote(
                feedbackId = item.feedbackId,
                score = item.score,
                comment = item.comment,
                latitude = item.latitude,
                longitude = item.longitude,
                line = item.line,
                createdAt = item.createdAt,
                transportType = item.transportType,
                crowdingScore = item.crowdingScore,
                cleanlinessScore = item.cleanlinessScore,
                punctualityScore = item.punctualityScore,
                createdByUserId = item.createdByUserId
            )
            if (updated == 0) {
                insertRemote(item)
            }
        }

        val serverIds = items.map { it.feedbackId }
        if (serverIds.isEmpty()) {
            deleteAllSynced()
        } else {
            deleteSyncedNotInServer(serverIds)
        }
    }
}

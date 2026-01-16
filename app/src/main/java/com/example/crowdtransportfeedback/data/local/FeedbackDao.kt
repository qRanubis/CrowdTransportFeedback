package com.example.crowdtransportfeedback.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import androidx.room.Update

@Dao
interface FeedbackDao {

    @Query("SELECT * FROM feedback ORDER BY createdAt DESC")
    fun getAll(): Flow<List<FeedbackEntity>>

    @Query("SELECT * FROM feedback WHERE id = :id")
    fun getById(id: Long): Flow<FeedbackEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FeedbackEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FeedbackEntity>)

    @Query("UPDATE feedback SET synced = :synced WHERE id = :id")
    suspend fun setSynced(id: Long, synced: Boolean)

    @Query("DELETE FROM feedback WHERE synced = 1")
    suspend fun deleteAllSynced()
    @Query("DELETE FROM feedback")
    suspend fun deleteAll()
    @Query("DELETE FROM feedback WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM feedback WHERE synced = 1 AND id NOT IN (:serverIds)")
    suspend fun deleteSyncedNotInServer(serverIds: List<Long>)

}

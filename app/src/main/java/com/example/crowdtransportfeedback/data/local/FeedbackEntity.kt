package com.example.crowdtransportfeedback.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "feedback",
    indices = [Index(value = ["feedbackId"], unique = true)]
)
data class FeedbackEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,

    val feedbackId: String = UUID.randomUUID().toString(),

    val score: Int,
    val comment: String,

    val latitude: Double?,
    val longitude: Double?,

    val line: String?,
    val createdAt: Long,

    val syncState: SyncState = SyncState.PENDING_CREATE
)

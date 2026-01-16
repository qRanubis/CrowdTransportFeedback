package com.example.crowdtransportfeedback.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feedback")
data class FeedbackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val score: Int,
    val comment: String,

    val latitude: Double?,
    val longitude: Double?,

    val line: String?,
    val createdAt: Long,

    val synced: Boolean
)

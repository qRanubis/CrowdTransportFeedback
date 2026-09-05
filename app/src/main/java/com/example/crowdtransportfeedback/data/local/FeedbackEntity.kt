package com.example.crowdtransportfeedback.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.crowdtransportfeedback.domain.TransportType
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
    val syncState: SyncState = SyncState.PENDING_CREATE,
    val transportType: TransportType? = null,
    val crowdingScore: Int? = null,
    val cleanlinessScore: Int? = null,
    val punctualityScore: Int? = null,
    val createdByUserId: String? = null,
    val createdByUsername: String? = null,
    val createdByAvatarKey: String? = null,
    val rejectionReason: String? = null
) {
    fun overallRating(): Double {
        val ratings = listOf(punctualityScore, cleanlinessScore, crowdingScore)
        return if (ratings.all { it != null }) {
            ratings.filterNotNull().average()
        } else {
            score.toDouble()
        }
    }

    fun isVisibleTo(currentUserId: String?): Boolean =
        syncState == SyncState.SYNCED ||
            (currentUserId != null && createdByUserId == currentUserId)
}

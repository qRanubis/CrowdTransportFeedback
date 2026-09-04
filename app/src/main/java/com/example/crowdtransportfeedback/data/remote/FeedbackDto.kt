package com.example.crowdtransportfeedback.data.remote

import com.example.crowdtransportfeedback.domain.TransportType

data class FeedbackDto(
    val id: String,
    val score: Double,
    val comment: String?,
    val line: String?,
    val createdAt: Long,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val transportType: TransportType? = null,
    val crowdingScore: Int? = null,
    val cleanlinessScore: Int? = null,
    val punctualityScore: Int? = null,
    val createdByUserId: String? = null,
    val createdByUsername: String? = null,
    val overallRating: Double? = null,
    val createdByAvatarKey: String? = null,
    val xpAwarded: Int = 0,
    val newAchievements: List<String> = emptyList()
)

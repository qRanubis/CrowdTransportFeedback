package com.example.crowdtransportfeedback.data.remote

import com.example.crowdtransportfeedback.domain.TransportType

data class FeedbackDto(
    val id: String,
    val score: Int,
    val comment: String?,
    val line: String?,
    val createdAt: Long,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val transportType: TransportType? = null,
    val crowdingScore: Int? = null,
    val cleanlinessScore: Int? = null,
    val punctualityScore: Int? = null,
    val createdByUserId: String? = null
)

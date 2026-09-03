package com.example.crowdtransportfeedback.data.remote

data class FeedbackDto(
    val id: String,
    val score: Int,
    val comment: String,
    val line: String?,
    val createdAt: Long,
    val latitude: Double? = null,
    val longitude: Double? = null
)



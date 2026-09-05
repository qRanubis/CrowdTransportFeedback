package com.example.crowdtransportfeedback.data.remote

import com.example.crowdtransportfeedback.data.local.FeedbackEntity
import com.example.crowdtransportfeedback.data.local.SyncState
import kotlin.math.roundToInt

fun FeedbackEntity.toDto(): FeedbackDto = FeedbackDto(
    id = feedbackId,
    score = score.toDouble(),
    comment = comment,
    line = line,
    createdAt = createdAt,
    latitude = latitude,
    longitude = longitude,
    transportType = transportType,
    crowdingScore = crowdingScore,
    cleanlinessScore = cleanlinessScore,
    punctualityScore = punctualityScore,
    createdByUserId = createdByUserId,
    createdByUsername = createdByUsername,
    overallRating = overallRating()
)

fun FeedbackDto.toEntity(syncState: SyncState = SyncState.SYNCED): FeedbackEntity =
    FeedbackEntity(
        feedbackId = id,
        score = score.roundToInt(),
        comment = comment.orEmpty(),
        latitude = latitude,
        longitude = longitude,
        line = line,
        createdAt = createdAt,
        syncState = syncState,
        transportType = transportType,
        crowdingScore = crowdingScore,
        cleanlinessScore = cleanlinessScore,
        punctualityScore = punctualityScore,
        createdByUserId = createdByUserId,
        createdByUsername = createdByUsername,
        createdByAvatarKey = createdByAvatarKey
    )

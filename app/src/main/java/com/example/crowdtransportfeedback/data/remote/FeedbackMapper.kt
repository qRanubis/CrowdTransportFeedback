package com.example.crowdtransportfeedback.data.remote

import com.example.crowdtransportfeedback.data.local.FeedbackEntity
import com.example.crowdtransportfeedback.data.local.SyncState

fun FeedbackEntity.toDto(): FeedbackDto = FeedbackDto(
    id = feedbackId,
    score = score,
    comment = comment,
    line = line,
    createdAt = createdAt,
    latitude = latitude,
    longitude = longitude,
    transportType = transportType,
    crowdingScore = crowdingScore,
    cleanlinessScore = cleanlinessScore,
    punctualityScore = punctualityScore,
    createdByUserId = createdByUserId
)

fun FeedbackDto.toEntity(syncState: SyncState = SyncState.SYNCED): FeedbackEntity = FeedbackEntity(
    feedbackId = id,
    score = score,
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
    createdByUserId = createdByUserId
)

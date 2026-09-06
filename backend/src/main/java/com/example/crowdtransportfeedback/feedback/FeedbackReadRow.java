package com.example.crowdtransportfeedback.feedback;

import java.util.UUID;

/** Immutable scalar projection used only by complete feedback retrieval. */
public record FeedbackReadRow(
    UUID feedbackId,
    UUID ownerId,
    String ownerUsername,
    String ownerAvatarKey,
    TransportType transportType,
    String line,
    double score,
    int punctualityScore,
    int cleanlinessScore,
    int crowdingScore,
    String comment,
    double latitude,
    double longitude,
    long createdAt
) {}

package com.example.crowdtransportfeedback.feedback;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class FeedbackDtos {
    private FeedbackDtos() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Request(
        @NotNull @JsonAlias("id") UUID feedbackId,
        @NotNull TransportType transportType,
        @NotBlank @Size(max = 32) String line,
        Integer score,
        @NotNull @Min(1) @Max(5) Integer punctualityScore,
        @NotNull @Min(1) @Max(5) Integer cleanlinessScore,
        @NotNull @Min(1) @Max(5) Integer crowdingScore,
        @Size(max = 2000) String comment,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotNull @PositiveOrZero Long createdAt
    ) {}

    public record Response(
        UUID feedbackId,
        String id,
        UUID createdByUserId,
        String createdByUsername,
        TransportType transportType,
        String line,
        int score,
        double overallRating,
        int punctualityScore,
        int cleanlinessScore,
        int crowdingScore,
        String comment,
        double latitude,
        double longitude,
        long createdAt
    ) {}
}

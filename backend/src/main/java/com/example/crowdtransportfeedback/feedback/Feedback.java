package com.example.crowdtransportfeedback.feedback;

import com.example.crowdtransportfeedback.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "feedback")
public class Feedback {
    @Id
    @Column(name = "feedback_id")
    public UUID feedbackId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    public AppUser owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_type", nullable = false, length = 24)
    public TransportType transportType;

    @Column(nullable = false, length = 32)
    public String line;

    @Column(name = "normalized_line", nullable = false, length = 32)
    public String normalizedLine;

    @Column(name = "score", nullable = false)
    public double score;

    @Column(name = "punctuality_score", nullable = false)
    public int punctualityScore;

    @Column(name = "cleanliness_score", nullable = false)
    public int cleanlinessScore;

    @Column(name = "crowding_score", nullable = false)
    public int crowdingScore;

    @Column(length = 2000)
    public String comment;

    @Column(nullable = false)
    public double latitude;

    @Column(nullable = false)
    public double longitude;

    @Column(name = "created_at", nullable = false)
    public long createdAt;

    protected Feedback() {}

    public Feedback(UUID id, AppUser owner, TransportType type, String line, int punctuality,
                    int cleanliness, int crowding, String comment, double latitude,
                    double longitude, long createdAt) {
        this.feedbackId = id; this.owner = owner; this.transportType = type; this.line = line;
        this.normalizedLine = line.trim().toUpperCase(java.util.Locale.ROOT);
        this.punctualityScore = punctuality; this.cleanlinessScore = cleanliness;
        this.crowdingScore = crowding; this.score = (punctuality + cleanliness + crowding) / 3.0;
        this.comment = comment; this.latitude = latitude; this.longitude = longitude; this.createdAt = createdAt;
    }
}

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

    @Column(name = "score", nullable = false)
    public int score;

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
}

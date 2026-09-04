package com.example.crowdtransportfeedback.feedback;
import com.example.crowdtransportfeedback.user.AppUser; import jakarta.persistence.*; import java.util.UUID;
@Entity @Table(name="feedback") public class Feedback {
 @Id @Column(name="feedback_id") public UUID feedbackId; @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="created_by_user_id") public AppUser owner;
 @Column(name="transport_type",length=24) public String transportType; @Column(length=32) public String line; @Column(name="score",nullable=false) public int score;
 @Column(name="punctuality_score") public Integer punctualityScore; @Column(name="cleanliness_score") public Integer cleanlinessScore; @Column(name="crowding_score") public Integer crowdingScore;
 @Column(length=2000) public String comment; public Double latitude; public Double longitude; @Column(name="created_at",nullable=false) public long createdAt;
 protected Feedback(){}
}

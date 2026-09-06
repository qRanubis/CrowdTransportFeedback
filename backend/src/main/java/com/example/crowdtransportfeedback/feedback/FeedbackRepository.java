package com.example.crowdtransportfeedback.feedback;
import java.util.List; import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
public interface FeedbackRepository extends JpaRepository<Feedback,UUID>{
 @Query("""
  select new com.example.crowdtransportfeedback.feedback.FeedbackReadRow(
   f.feedbackId, f.owner.id, f.owner.username, f.owner.avatarKey,
   f.transportType, f.line, f.score, f.punctualityScore,
   f.cleanlinessScore, f.crowdingScore, f.comment, f.latitude,
   f.longitude, f.createdAt)
  from Feedback f
  """)
 List<FeedbackReadRow> findAllReadRows();
 List<Feedback> findByOwnerId(UUID userId);
}

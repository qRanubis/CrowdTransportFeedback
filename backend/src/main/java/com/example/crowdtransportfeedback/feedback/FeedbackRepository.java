package com.example.crowdtransportfeedback.feedback;
import java.util.List; import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
public interface FeedbackRepository extends JpaRepository<Feedback,UUID>{
 @Query("select f from Feedback f join fetch f.owner")
 List<Feedback> findAllWithOwner();
 List<Feedback> findByOwnerId(UUID userId);
}

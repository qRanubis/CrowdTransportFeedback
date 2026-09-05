package com.example.crowdtransportfeedback.moderation;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface FeedbackReportRepository extends JpaRepository<FeedbackReport,UUID>{
 Optional<FeedbackReport> findByFeedbackIdAndReporterId(UUID feedbackId,UUID reporterId);
 List<FeedbackReport> findByFeedbackIdAndStatus(UUID feedbackId,ReportStatus status);
 long countByReporterIdAndStatus(UUID reporterId,ReportStatus status);
}

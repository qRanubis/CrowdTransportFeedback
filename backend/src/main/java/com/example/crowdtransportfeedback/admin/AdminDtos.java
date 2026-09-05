package com.example.crowdtransportfeedback.admin;
import com.example.crowdtransportfeedback.feedback.FeedbackDtos; import java.time.Instant; import java.util.*;
public final class AdminDtos {
 public record Page<T>(List<T> content,int page,int size,long totalElements,int totalPages){}
 public record TopLine(String transportType,String line,long count){}
 public record Overview(long totalUsers,long totalFeedbacks,long feedbackLast24h,long feedbackLast7d,long feedbackLast30d,long activeContributors30d,long pendingReports,long reportedFeedbackAwaitingReview,Map<String,Long> feedbackByTransportType,List<TopLine> topLines){}
 public record QueueItem(UUID feedbackId,String authorUsername,String transportType,String line,double score,long reportCount,Instant lastReportedAt,Map<String,Long> reasonCounts){}
 public record PendingReport(UUID id,String reporterUsername,String reason,String details,Instant createdAt){}
 public record ModerationDetail(FeedbackDtos.Response feedback,List<PendingReport> reports){}
 public record ResolveRequest(String action,String note){}
 public record AdminFeedback(UUID feedbackId,long createdAt,String username,String transportType,String line,double score,String comment){}
 public record AdminUser(UUID id,String username,String role,Instant joinedAt,long feedbackCount,long totalXp,int level,long verifiedReportCount){}
 public record ReportingSummary(long feedbackCount,long uniqueContributors,Double averageOverall,Double averagePunctuality,Double averageCleanliness,Double averageCrowding,String mostActiveTransportType,String mostActiveLine){}
 private AdminDtos(){}
}

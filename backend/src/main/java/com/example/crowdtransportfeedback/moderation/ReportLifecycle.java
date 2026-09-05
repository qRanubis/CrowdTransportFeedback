package com.example.crowdtransportfeedback.moderation;
import com.example.crowdtransportfeedback.gamification.GamificationService; import com.example.crowdtransportfeedback.user.*; import java.time.Instant; import java.util.UUID; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class ReportLifecycle {
 private final FeedbackReportRepository reports; private final AdminAuditLogRepository audit; private final UserRepository users; private final GamificationService gamification;
 public ReportLifecycle(FeedbackReportRepository r,AdminAuditLogRepository a,UserRepository u,GamificationService g){reports=r;audit=a;users=u;gamification=g;}
 @Transactional public int resolve(UUID feedbackId,UUID adminId,boolean confirmed,String note){var pending=reports.findByFeedbackIdAndStatus(feedbackId,ReportStatus.PENDING);Instant now=Instant.now();var admin=users.getReferenceById(adminId);for(var r:pending){r.status=confirmed?ReportStatus.CONFIRMED:ReportStatus.DISMISSED;r.resolvedAt=now;r.resolvedBy=admin;}reports.saveAll(pending);if(confirmed)pending.stream().map(r->r.reporter).distinct().forEach(u->gamification.evaluate(u,now));audit.save(new AdminAuditLog(admin,confirmed?"REPORTED_FEEDBACK_DELETED":"REPORTS_DISMISSED",feedbackId,note));return pending.size();}
 @Transactional public void close(UUID feedbackId){var pending=reports.findByFeedbackIdAndStatus(feedbackId,ReportStatus.PENDING);Instant now=Instant.now();pending.forEach(r->{r.status=ReportStatus.CLOSED;r.resolvedAt=now;});reports.saveAll(pending);}
 public boolean hasPending(UUID feedbackId){return !reports.findByFeedbackIdAndStatus(feedbackId,ReportStatus.PENDING).isEmpty();}
 @Transactional public void auditDelete(UUID feedbackId,UUID adminId){audit.save(new AdminAuditLog(users.getReferenceById(adminId),"FEEDBACK_DELETED",feedbackId,null));}
}

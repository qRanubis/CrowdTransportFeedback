package com.example.crowdtransportfeedback.moderation;
import com.example.crowdtransportfeedback.user.AppUser;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="feedback_report", uniqueConstraints=@UniqueConstraint(columnNames={"feedback_id","reporter_user_id"}))
public class FeedbackReport {
 @Id public UUID id;
 @Column(name="feedback_id",nullable=false) public UUID feedbackId;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="reporter_user_id") public AppUser reporter;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=40) public ReportReason reason;
 @Column(length=250) public String details;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) public ReportStatus status;
 @Column(name="created_at",nullable=false) public Instant createdAt;
 @Column(name="resolved_at") public Instant resolvedAt;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="resolved_by_user_id") public AppUser resolvedBy;
 protected FeedbackReport(){}
 public FeedbackReport(UUID feedbackId,AppUser reporter,ReportReason reason,String details){this.id=UUID.randomUUID();this.feedbackId=feedbackId;this.reporter=reporter;this.reason=reason;this.details=details;this.status=ReportStatus.PENDING;this.createdAt=Instant.now();}
}

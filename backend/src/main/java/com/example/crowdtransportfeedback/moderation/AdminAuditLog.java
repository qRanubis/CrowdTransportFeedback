package com.example.crowdtransportfeedback.moderation;
import com.example.crowdtransportfeedback.user.AppUser; import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="admin_audit_log") public class AdminAuditLog {
 @Id public UUID id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="admin_user_id") public AppUser admin;
 @Column(nullable=false,length=40) public String action; @Column(name="target_type",nullable=false,length=32) public String targetType;
 @Column(name="target_id",nullable=false) public UUID targetId; @Column(length=1000) public String details; @Column(name="created_at",nullable=false) public Instant createdAt;
 protected AdminAuditLog(){} public AdminAuditLog(AppUser a,String action,UUID target,String details){id=UUID.randomUUID();admin=a;this.action=action;targetType="FEEDBACK";targetId=target;this.details=details;createdAt=Instant.now();}
}

package com.example.crowdtransportfeedback.auth;
import com.example.crowdtransportfeedback.user.AppUser; import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="refresh_session") public class RefreshSession {
 @Id public UUID id; @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="user_id") public AppUser user;
 @Column(name="token_hash",nullable=false,unique=true,length=64) public String tokenHash; @Column(name="created_at",nullable=false) public Instant createdAt;
 @Column(name="expires_at",nullable=false) public Instant expiresAt; @Column(name="last_used_at",nullable=false) public Instant lastUsedAt;
 @Column(name="revoked_at") public Instant revokedAt; @Column(name="replaced_by") public UUID replacedBy;
 protected RefreshSession(){} RefreshSession(UUID id,AppUser u,String hash,Instant now,Instant expires){this.id=id;user=u;tokenHash=hash;createdAt=now;lastUsedAt=now;expiresAt=expires;}
}

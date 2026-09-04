package com.example.crowdtransportfeedback.gamification;
import com.example.crowdtransportfeedback.user.AppUser;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="gamification_event", uniqueConstraints=@UniqueConstraint(columnNames={"user_id","event_type","source_key"}))
public class GamificationEvent {
 @Id public UUID id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="user_id") public AppUser user;
 @Column(name="event_type",nullable=false) public String type;
 @Column(name="source_key",nullable=false) public String sourceKey;
 @Column(name="xp_delta",nullable=false) public int xpDelta;
 @Column(name="line_identity") public String lineIdentity;
 @Column(name="created_at",nullable=false) public Instant createdAt;
 protected GamificationEvent(){}
 public GamificationEvent(AppUser user,String type,String source,int xp,String identity,Instant at){id=UUID.randomUUID();this.user=user;this.type=type;sourceKey=source;xpDelta=xp;lineIdentity=identity;createdAt=at;}
}

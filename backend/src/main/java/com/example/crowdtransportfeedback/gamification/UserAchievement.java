package com.example.crowdtransportfeedback.gamification;
import com.example.crowdtransportfeedback.user.AppUser; import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="user_achievement",uniqueConstraints=@UniqueConstraint(columnNames={"user_id","achievement_code"}))
public class UserAchievement { @Id public UUID id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id") public AppUser user; @Column(name="achievement_code") public String code; @Column(name="unlocked_at") public Instant unlockedAt; protected UserAchievement(){} public UserAchievement(AppUser u,String c,Instant at){id=UUID.randomUUID();user=u;code=c;unlockedAt=at;} }

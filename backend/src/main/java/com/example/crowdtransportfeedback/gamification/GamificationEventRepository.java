package com.example.crowdtransportfeedback.gamification;
import java.time.Instant; import java.util.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface GamificationEventRepository extends JpaRepository<GamificationEvent,UUID>{
 boolean existsByUserIdAndTypeAndSourceKey(UUID userId,String type,String sourceKey);
 @Query("select coalesce(sum(e.xpDelta),0) from GamificationEvent e where e.user.id=:id") long total(@Param("id") UUID id);
 @Query("select count(e)>0 from GamificationEvent e where e.user.id=:id and e.type='FEEDBACK_BASE_AWARDED' and e.lineIdentity=:line and e.createdAt>:after and e.createdAt<=:at") boolean cooldown(@Param("id") UUID id,@Param("line") String line,@Param("after") Instant after,@Param("at") Instant at);
 List<GamificationEvent> findByUserId(UUID id);
}

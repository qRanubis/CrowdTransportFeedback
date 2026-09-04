package com.example.crowdtransportfeedback.auth;
import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import jakarta.persistence.LockModeType; import java.util.*;
public interface RefreshSessionRepository extends JpaRepository<RefreshSession,UUID>{
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from RefreshSession s join fetch s.user where s.tokenHash=:hash") Optional<RefreshSession> findByTokenHash(@Param("hash")String hash);
}

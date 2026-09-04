package com.example.crowdtransportfeedback.gamification;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface UserAchievementRepository extends JpaRepository<UserAchievement,UUID>{ List<UserAchievement> findByUserId(UUID id); boolean existsByUserIdAndCode(UUID id,String code); long countByUserId(UUID id); }

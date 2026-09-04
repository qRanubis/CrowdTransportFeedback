package com.example.crowdtransportfeedback.gamification;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface UserAchievementRepository extends JpaRepository<UserAchievement,UUID>{ List<UserAchievement> findByUserId(UUID id); Optional<UserAchievement> findByUserIdAndCode(UUID id,String code); boolean existsByUserIdAndCode(UUID id,String code); long countByUserId(UUID id); }

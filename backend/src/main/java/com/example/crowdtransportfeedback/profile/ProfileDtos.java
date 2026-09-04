package com.example.crowdtransportfeedback.profile;
import java.time.Instant; import java.util.*;
public final class ProfileDtos {private ProfileDtos(){}
 public record Level(int level,String title,long levelStartXp,long xpIntoLevel,Long xpNeededForNextLevel,Integer nextLevelThreshold,boolean maxLevel){}
 public record Badge(String code,String title,String description,String category,boolean unlocked,Instant unlockedAt,long currentProgress,int targetProgress,boolean pinned,Integer pinOrder){}
 public record PublicProfile(String username,String avatarKey,long totalXp,Level level,long contributionCount,long differentLineCount,long transportTypeCount,long unlockedAchievementCount,List<Badge> pinnedAchievements){}
 public record MyProfile(String username,String avatarKey,long totalXp,Level level,long contributionCount,long differentLineCount,long transportTypeCount,long unlockedAchievementCount,int achievementTotal,Long allTimeXpRank,List<Badge> pinnedAchievements,Map<String,Long> contributionBreakdown){}
 public record AvatarRequest(String avatarKey){} public record PinsRequest(List<String> achievementCodes){}
}

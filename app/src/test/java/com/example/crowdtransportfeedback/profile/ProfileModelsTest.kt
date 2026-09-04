package com.example.crowdtransportfeedback.profile
import org.junit.Assert.*; import org.junit.Test
class ProfileModelsTest {
 @Test fun maxLevelHasNoNextThreshold(){val level=LevelDto(15,"Urban Mobility Expert",2420,null,true);assertTrue(level.maxLevel);assertNull(level.nextLevelXp)}
 @Test fun leaderboardKeepsSeparateCurrentUserOutsideTop100(){val me=LeaderboardEntryDto(137,"rares","COMMUTER",8,1520,1520,true);val model=LeaderboardDto(emptyList(),me);assertEquals(137,model.currentUser.rank);assertTrue(model.top.isEmpty())}
 @Test fun achievementCarriesServerProgressAndLockState(){val badge=BadgeDto("LINE_EXPLORER","Line Explorer","Contribute on 10 lines","Network Exploration",false,null,7,10,false);assertFalse(badge.unlocked);assertEquals(7,badge.currentProgress)}
}

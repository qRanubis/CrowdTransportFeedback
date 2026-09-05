package com.example.crowdtransportfeedback.gamification;
import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.*;
class LevelCatalogTest {
 @Test void everyThresholdAndBoundaryMapsToExpectedLevel(){for(var l:LevelCatalog.LEVELS){assertEquals(l.level(),LevelCatalog.forXp(l.threshold()).level());if(l.threshold()>0)assertEquals(l.level()-1,LevelCatalog.forXp(l.threshold()-1).level());}}
 @Test void xpContinuesAboveMaximum(){assertEquals(15,LevelCatalog.forXp(7420).level());assertNull(LevelCatalog.nextThreshold(7420));}
 @Test void reversibleDeletionCanRecalculateLevelDown(){assertEquals(2,LevelCatalog.forXp(109).level());assertEquals(1,LevelCatalog.forXp(109-10).level());}
 @Test void catalogHasExactlyThirtyOneZeroXpAchievements(){assertEquals(31,AchievementCatalog.ALL.size());assertEquals(31,AchievementCatalog.ALL.stream().map(AchievementCatalog.Definition::code).distinct().count());}
}

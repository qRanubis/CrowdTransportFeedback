package com.example.crowdtransportfeedback.gamification;
import static org.junit.jupiter.api.Assertions.*; import org.junit.jupiter.api.Test;
class ModerationAchievementCatalogTest {
 @Test void verifiedReportAchievementsHaveRequiredThresholdsAndNoXpEvents(){assertEquals(1,target("WATCHFUL_COMMUTER"));assertEquals(5,target("COMMUNITY_GUARDIAN"));assertEquals(15,target("TRUSTED_REPORTER"));assertTrue(AchievementCatalog.ALL.stream().filter(d->d.code().equals("WATCHFUL_COMMUTER")).allMatch(d->d.kind()==AchievementCatalog.Kind.VERIFIED_REPORTS));}
 private int target(String code){return AchievementCatalog.ALL.stream().filter(d->d.code().equals(code)).findFirst().orElseThrow().target();}
}

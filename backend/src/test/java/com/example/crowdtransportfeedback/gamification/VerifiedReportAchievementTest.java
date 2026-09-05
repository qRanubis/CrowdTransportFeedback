package com.example.crowdtransportfeedback.gamification;
import static org.junit.jupiter.api.Assertions.*; import static org.mockito.Mockito.*; import java.time.Instant; import java.util.*; import org.junit.jupiter.api.Test; import org.mockito.ArgumentCaptor;
import com.example.crowdtransportfeedback.feedback.FeedbackRepository; import com.example.crowdtransportfeedback.moderation.*; import com.example.crowdtransportfeedback.user.*;
class VerifiedReportAchievementTest {
 @Test void thresholdsUnlockWithoutXp(){assertUnlocked(1,"WATCHFUL_COMMUTER");assertUnlocked(5,"COMMUNITY_GUARDIAN");assertUnlocked(15,"TRUSTED_REPORTER");}
 @Test void nonConfirmedStatusesDoNotCount(){var fixture=fixture(0);assertTrue(fixture.service.evaluate(fixture.user,Instant.now()).isEmpty());verify(fixture.events,never()).save(any());verify(fixture.achievements,never()).save(any());}
 private void assertUnlocked(long confirmed,String expected){var f=fixture(confirmed);f.service.evaluate(f.user,Instant.now());ArgumentCaptor<UserAchievement> c=ArgumentCaptor.forClass(UserAchievement.class);verify(f.achievements,atLeastOnce()).save(c.capture());assertTrue(c.getAllValues().stream().anyMatch(a->a.code.equals(expected)));verify(f.events,never()).save(any());}
 private Fixture fixture(long confirmed){var events=mock(GamificationEventRepository.class);var achievements=mock(UserAchievementRepository.class);var feedback=mock(FeedbackRepository.class);var reports=mock(FeedbackReportRepository.class);var user=new AppUser(UUID.randomUUID(),"user@example.com","user123","hash",Role.USER,Instant.now());when(feedback.findByOwnerId(user.id)).thenReturn(List.of());when(reports.countByReporterIdAndStatus(user.id,ReportStatus.CONFIRMED)).thenReturn(confirmed);return new Fixture(new GamificationService(events,achievements,feedback,reports),events,achievements,user);}
 private record Fixture(GamificationService service,GamificationEventRepository events,UserAchievementRepository achievements,AppUser user){}
}

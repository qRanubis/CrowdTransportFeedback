package com.example.crowdtransportfeedback.gamification;
import static org.assertj.core.api.Assertions.assertThat; import static org.mockito.Mockito.mock;
import com.example.crowdtransportfeedback.feedback.FeedbackRepository; import com.example.crowdtransportfeedback.moderation.FeedbackReportRepository;
import org.junit.jupiter.api.Test; import org.springframework.boot.test.context.runner.ApplicationContextRunner; import org.springframework.context.annotation.*;
class GamificationServiceContextTest {
 private final ApplicationContextRunner runner=new ApplicationContextRunner().withBean(GamificationEventRepository.class,()->mock(GamificationEventRepository.class)).withBean(UserAchievementRepository.class,()->mock(UserAchievementRepository.class)).withBean(FeedbackRepository.class,()->mock(FeedbackRepository.class)).withBean(FeedbackReportRepository.class,()->mock(FeedbackReportRepository.class)).withUserConfiguration(Config.class);
 @Test void springSelectsProductionConstructor(){runner.run(context->{assertThat(context).hasNotFailed();assertThat(context).hasSingleBean(GamificationService.class);});}
 @Configuration(proxyBeanMethods=false) @Import(GamificationService.class) static class Config{}
}

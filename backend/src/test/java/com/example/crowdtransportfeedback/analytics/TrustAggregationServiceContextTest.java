package com.example.crowdtransportfeedback.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.crowdtransportfeedback.feedback.FeedbackRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class TrustAggregationServiceContextTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withBean(FeedbackRepository.class, () -> mock(FeedbackRepository.class))
        .withUserConfiguration(TestConfiguration.class);

    @Test
    void springSelectsTheProductionConstructor() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(TrustAggregationService.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(TrustAggregationService.class)
    static class TestConfiguration {}
}

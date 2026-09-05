package com.example.crowdtransportfeedback.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.crowdtransportfeedback.feedback.Feedback;
import com.example.crowdtransportfeedback.feedback.FeedbackRepository;
import com.example.crowdtransportfeedback.user.AppUser;
import com.example.crowdtransportfeedback.user.UserRepository;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class DemoDataSeedTest {
    @Test
    void generatedUsernamesMatchProductionRuleAndSeedCreatesExpectedCounts() throws Exception {
        UserRepository users = mock(UserRepository.class);
        FeedbackRepository feedback = mock(FeedbackRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(users.findById(any())).thenReturn(Optional.empty());
        when(users.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(feedback.existsById(any())).thenReturn(false);
        when(feedback.save(any(Feedback.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(encoder.encode(any())).thenReturn("encoded-demo-password");

        new DemoDataSeed(users, feedback, encoder).run(null);

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(users, times(8)).save(userCaptor.capture());
        verify(feedback, times(50)).save(any(Feedback.class));

        Set<String> usernames = new HashSet<>();
        for (AppUser user : userCaptor.getAllValues()) {
            assertThat(user.getUsername()).matches("^[a-z0-9]{3,20}$");
            usernames.add(user.getUsername());
        }
        assertThat(usernames).hasSize(8).containsExactlyInAnyOrder(
            "demo1", "demo2", "demo3", "demo4", "demo5", "demo6", "demo7", "demo8"
        );
    }
}

package com.example.crowdtransportfeedback.feedback;

import com.example.crowdtransportfeedback.common.ApiException;
import com.example.crowdtransportfeedback.user.AppUser;
import com.example.crowdtransportfeedback.user.Role;
import com.example.crowdtransportfeedback.user.UserRepository;
import com.example.crowdtransportfeedback.gamification.GamificationService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FeedbackServiceTest {
    FeedbackRepository repository = mock(FeedbackRepository.class);
    UserRepository users = mock(UserRepository.class);
    GamificationService gamification = mock(GamificationService.class);
    FeedbackService service = new FeedbackService(repository, users, gamification);
    UUID owner = UUID.randomUUID();
    UUID id = UUID.randomUUID();

    FeedbackDtos.Request request = new FeedbackDtos.Request(
        id, TransportType.BUS, "123", 1.0, 5, 4, 2, "comment", 44.4, 26.1, 100L
    );

    @BeforeEach
    void setup() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(users.getReferenceById(owner)).thenReturn(proxyLikeUser(owner, "owner1"));
        when(gamification.award(any(), any())).thenReturn(new GamificationService.Award(0, java.util.List.of()));
    }

    @Test
    void authenticatedUserBecomesOwnerAndOverallIsStoredWithOneDecimal() {
        var created = service.create(request, owner);
        assertEquals(owner, created.createdByUserId());
        assertEquals("owner1", created.createdByUsername());
        assertEquals(3.7, created.overallRating(), 0.0001);
        assertEquals(3.7, created.score(), 0.0001);
        verify(repository).save(any());
    }

    @Test
    void clientLegacyScoreDoesNotAffectIdempotency() {
        when(repository.findById(id)).thenReturn(Optional.of(entity(owner)));
        var differentClientScore = new FeedbackDtos.Request(
            id, TransportType.BUS, "123", 5.0, 5, 4, 2, "comment", 44.4, 26.1, 100L
        );
        assertEquals(id, service.create(differentClientScore, owner).feedbackId());
        verify(repository, never()).save(any());
    }

    @Test
    void anotherUserCannotClaimExistingId() {
        when(repository.findById(id)).thenReturn(Optional.of(entity(owner)));
        assertThrows(ApiException.class, () -> service.create(request, UUID.randomUUID()));
    }

    @Test
    void authorCanDeleteOwnFeedback() {
        Feedback stored = entity(owner);
        when(repository.findById(id)).thenReturn(Optional.of(stored));
        service.delete(id, owner, "USER");
        verify(repository).delete(stored);
    }

    @Test
    void adminCanDeleteAnotherUsersFeedback() {
        Feedback stored = entity(owner);
        when(repository.findById(id)).thenReturn(Optional.of(stored));
        service.delete(id, UUID.randomUUID(), "ADMIN");
        verify(repository).delete(stored);
    }

    @Test
    void unrelatedUserCannotDeleteFeedback() {
        when(repository.findById(id)).thenReturn(Optional.of(entity(owner)));
        assertEquals(
            "feedback_delete_forbidden",
            assertThrows(ApiException.class, () -> service.delete(id, UUID.randomUUID(), "USER")).code
        );
    }

    @Test
    void missingDeleteReturnsNotFound() {
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertEquals(
            "feedback_not_found",
            assertThrows(ApiException.class, () -> service.delete(id, owner, "USER")).code
        );
    }

    private Feedback entity(UUID userId) {
        Feedback feedback = new Feedback();
        feedback.feedbackId = id;
        feedback.owner = proxyLikeUser(userId, "owner1");
        feedback.transportType = TransportType.BUS;
        feedback.line = "123";
        feedback.score = 3.7;
        feedback.punctualityScore = 5;
        feedback.cleanlinessScore = 4;
        feedback.crowdingScore = 2;
        feedback.comment = "comment";
        feedback.latitude = 44.4;
        feedback.longitude = 26.1;
        feedback.createdAt = 100L;
        return feedback;
    }

    private AppUser proxyLikeUser(UUID userId, String username) {
        String expectedUsername = username;
        AppUser user = new AppUser(userId, "user@example.com", username, "hash", Role.USER, Instant.now()) {
            @Override public UUID getId() { return userId; }
            @Override public String getUsername() { return expectedUsername; }
        };
        user.id = null;
        user.username = null;
        return user;
    }
}

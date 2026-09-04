package com.example.crowdtransportfeedback.feedback;

import com.example.crowdtransportfeedback.common.ApiException;
import com.example.crowdtransportfeedback.user.AppUser;
import com.example.crowdtransportfeedback.user.Role;
import com.example.crowdtransportfeedback.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedbackServiceTest {
    FeedbackRepository repository = mock(FeedbackRepository.class);
    UserRepository users = mock(UserRepository.class);
    FeedbackService service = new FeedbackService(repository, users);
    UUID owner = UUID.randomUUID();
    UUID id = UUID.randomUUID();
    FeedbackDtos.Request request = new FeedbackDtos.Request(
        id,
        TransportType.BUS,
        "123",
        5,
        4,
        3,
        2,
        "comment",
        44.4,
        26.1,
        100L
    );

    @BeforeEach
    void setup() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(users.getReferenceById(owner)).thenReturn(proxyLikeUser(owner));
    }

    @Test
    void authenticatedUserBecomesOwnerAndGetByIdWorks() {
        var created = service.create(request, owner);
        assertEquals(owner, created.createdByUserId());
        verify(repository).save(any());

        var stored = entity(owner);
        when(repository.findById(id)).thenReturn(Optional.of(stored));
        assertEquals(id, service.get(id).feedbackId());
        assertEquals(owner, service.get(id).createdByUserId());
    }

    @Test
    void identicalOwnerRetryIsIdempotent() {
        when(repository.findById(id)).thenReturn(Optional.of(entity(owner)));
        assertEquals(id, service.create(request, owner).feedbackId());
        verify(repository, never()).save(any());
    }

    @Test
    void anotherUserCannotClaimExistingId() {
        when(repository.findById(id)).thenReturn(Optional.of(entity(owner)));
        assertThrows(ApiException.class, () -> service.create(request, UUID.randomUUID()));
    }

    @Test
    void missingDeleteReturnsNotFound() {
        when(repository.existsById(id)).thenReturn(false);
        assertEquals(
            "feedback_not_found",
            assertThrows(ApiException.class, () -> service.delete(id)).code
        );
    }

    private Feedback entity(UUID userId) {
        Feedback feedback = new Feedback();
        feedback.feedbackId = id;
        feedback.owner = proxyLikeUser(userId);
        feedback.transportType = TransportType.BUS;
        feedback.line = "123";
        feedback.score = 5;
        feedback.punctualityScore = 4;
        feedback.cleanlinessScore = 3;
        feedback.crowdingScore = 2;
        feedback.comment = "comment";
        feedback.latitude = 44.4;
        feedback.longitude = 26.1;
        feedback.createdAt = 100L;
        return feedback;
    }

    private AppUser proxyLikeUser(UUID userId) {
        AppUser user = new AppUser(userId, "user@example.com", "hash", Role.USER, Instant.now()) {
            @Override
            public UUID getId() {
                return userId;
            }
        };
        user.id = null;
        return user;
    }
}

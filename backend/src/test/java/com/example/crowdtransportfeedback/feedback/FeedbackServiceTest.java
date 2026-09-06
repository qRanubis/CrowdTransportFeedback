package com.example.crowdtransportfeedback.feedback;

import com.example.crowdtransportfeedback.common.ApiException;
import com.example.crowdtransportfeedback.user.AppUser;
import com.example.crowdtransportfeedback.user.Role;
import com.example.crowdtransportfeedback.user.UserRepository;
import com.example.crowdtransportfeedback.gamification.GamificationService;
import com.example.crowdtransportfeedback.moderation.ReportLifecycle;
import java.time.Instant;
import java.util.List;
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
    ReportLifecycle reports = mock(ReportLifecycle.class);
    FeedbackService service = new FeedbackService(repository, users, gamification, reports);
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
        assertEquals("NAVIGATOR", created.createdByAvatarKey());
        assertEquals(3.7, created.overallRating(), 0.0001);
        assertEquals(3.7, created.score(), 0.0001);
        verify(repository).save(any());
    }

    @Test
    void allUsesReadProjectionAndPreservesCompleteResponseContract() {
        FeedbackReadRow first = readRow(id, owner);
        UUID secondId = UUID.randomUUID();
        UUID secondOwner = UUID.randomUUID();
        FeedbackReadRow second = readRow(secondId, secondOwner);
        when(repository.findAllReadRows()).thenReturn(List.of(first, second));

        var responses = service.all();

        assertEquals(2, responses.size());
        assertEquals(id, responses.get(0).feedbackId());
        var response = responses.get(1);
        assertEquals(secondId, response.feedbackId());
        assertEquals(secondId.toString(), response.id());
        assertEquals(secondOwner, response.createdByUserId());
        assertEquals("owner1", response.createdByUsername());
        assertEquals("NAVIGATOR", response.createdByAvatarKey());
        assertEquals(TransportType.TRAM, response.transportType());
        assertEquals("41", response.line());
        assertEquals(4.3, response.score(), 0.0001);
        assertEquals(4.3, response.overallRating(), 0.0001);
        assertEquals(4, response.punctualityScore());
        assertEquals(5, response.cleanlinessScore());
        assertEquals(4, response.crowdingScore());
        assertEquals("second", response.comment());
        assertEquals(44.4268, response.latitude(), 0.0001);
        assertEquals(26.1025, response.longitude(), 0.0001);
        assertEquals(200L, response.createdAt());
        assertEquals(0, response.xpAwarded());
        assertEquals(List.of(), response.newAchievements());
        verify(repository).findAllReadRows();
        verify(repository, never()).findAll();
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
        verify(gamification).revoke(stored.owner, id);
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


    @Test
    void authorDeleteClosesPendingReports() {
        Feedback stored = entity(owner); when(repository.findById(id)).thenReturn(Optional.of(stored));
        service.delete(id, owner, "USER");
        verify(reports).close(id); verify(repository).delete(stored); verify(gamification).revoke(stored.owner, id);
    }

    @Test
    void adminDirectDeleteConfirmsPendingReports() {
        UUID admin = UUID.randomUUID(); Feedback stored = entity(owner); when(repository.findById(id)).thenReturn(Optional.of(stored)); when(reports.hasPending(id)).thenReturn(true);
        service.delete(id, admin, "ADMIN");
        verify(reports).resolve(id, admin, true, "Direct administrator deletion"); verify(repository).delete(stored);
    }

    @Test
    void moderationDeletePreservesSuppliedNote() {
        UUID admin = UUID.randomUUID(); Feedback stored = entity(owner); when(repository.findById(id)).thenReturn(Optional.of(stored)); when(reports.hasPending(id)).thenReturn(true);
        service.delete(id, admin, "ADMIN", "reviewed evidence");
        verify(reports).resolve(id, admin, true, "reviewed evidence");
    }

    @Test
    void adminDeleteWithoutReportsWritesNormalAudit() {
        UUID admin = UUID.randomUUID(); Feedback stored = entity(owner); when(repository.findById(id)).thenReturn(Optional.of(stored));
        service.delete(id, admin, "ADMIN"); verify(reports).auditDelete(id, admin);
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

    private FeedbackReadRow readRow(UUID feedbackId, UUID userId) {
        return new FeedbackReadRow(
            feedbackId, userId, "owner1", "NAVIGATOR", TransportType.TRAM,
            "41", 4.3, 4, 5, 4, "second", 44.4268, 26.1025, 200L
        );
    }

    private AppUser proxyLikeUser(UUID userId, String username) {
        String expectedUsername = username;
        AppUser user = new AppUser(userId, "user@example.com", username, "hash", Role.USER, Instant.now()) {
            @Override public UUID getId() { return userId; }
            @Override public String getUsername() { return expectedUsername; }
            @Override public String getAvatarKey() { return "NAVIGATOR"; }
        };
        user.id = null;
        user.username = null;
        user.avatarKey = "COMMUTER";
        return user;
    }
}

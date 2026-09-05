package com.example.crowdtransportfeedback.feedback;

import com.example.crowdtransportfeedback.common.ApiException;
import com.example.crowdtransportfeedback.user.UserRepository;
import com.example.crowdtransportfeedback.gamification.GamificationService;
import com.example.crowdtransportfeedback.moderation.ReportLifecycle;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import static com.example.crowdtransportfeedback.feedback.FeedbackDtos.Request;
import static com.example.crowdtransportfeedback.feedback.FeedbackDtos.Response;

@Service
public class FeedbackService {
    private final FeedbackRepository feedback;
    private final UserRepository users;
    private final GamificationService gamification;
    private final ReportLifecycle reports;

    @Autowired FeedbackService(FeedbackRepository feedback, UserRepository users, GamificationService gamification, ReportLifecycle reports) {
        this.feedback = feedback;
        this.users = users;
        this.gamification = gamification;
        this.reports = reports;
    }

    FeedbackService(FeedbackRepository feedback, UserRepository users, GamificationService gamification) {
        this(feedback, users, gamification, null);
    }

    @Transactional(readOnly = true)
    public List<Response> all() {
        return feedback.findAll().stream().map(this::out).toList();
    }

    @Transactional(readOnly = true)
    public Response get(UUID id) {
        return out(feedback.findById(id).orElseThrow(() ->
            new ApiException(HttpStatus.NOT_FOUND, "feedback_not_found", "Feedback was not found")
        ));
    }

    @Transactional
    public Response create(Request request, UUID userId) {
        var found = feedback.findById(request.feedbackId());
        if (found.isPresent()) {
            Feedback existing = found.get();
            if (!existing.owner.getId().equals(userId) || !equivalent(existing, request)) {
                throw new ApiException(
                    HttpStatus.CONFLICT,
                    "feedback_id_conflict",
                    "Feedback ID is already in use"
                );
            }
            return out(existing);
        }

        Feedback entity = new Feedback();
        entity.feedbackId = request.feedbackId();
        entity.owner = users.getReferenceById(userId);
        entity.transportType = request.transportType();
        entity.line = request.line().trim();
        entity.normalizedLine = GamificationService.normalizeLine(request.line());
        gamification.enforceCooldown(userId, entity.transportType + ":" + entity.normalizedLine, request.createdAt());
        entity.punctualityScore = request.punctualityScore();
        entity.cleanlinessScore = request.cleanlinessScore();
        entity.crowdingScore = request.crowdingScore();
        entity.score = overall(
            entity.punctualityScore,
            entity.cleanlinessScore,
            entity.crowdingScore
        );
        entity.comment = normalizeComment(request.comment());
        entity.latitude = request.latitude();
        entity.longitude = request.longitude();
        entity.createdAt = request.createdAt();
        Feedback saved=feedback.save(entity);
        var award=gamification.award(saved.owner,saved);
        return out(saved,award.xpAwarded(),award.newAchievements());
    }

    @Transactional
    public void delete(UUID id, UUID requesterId, String requesterRole) {
        Feedback entity = feedback.findById(id).orElseThrow(() ->
            new ApiException(HttpStatus.NOT_FOUND, "feedback_not_found", "Feedback was not found")
        );

        boolean admin = "ADMIN".equals(requesterRole);
        boolean owner = entity.owner.getId().equals(requesterId);
        if (!admin && !owner) {
            throw new ApiException(
                HttpStatus.FORBIDDEN,
                "feedback_delete_forbidden",
                "Only the author or an administrator can delete this feedback"
            );
        }

        if (reports != null && admin) {
            if (reports.hasPending(id)) reports.resolve(id, requesterId, true, "Direct administrator deletion");
            else reports.auditDelete(id, requesterId);
        } else if (reports != null) reports.close(id);
        gamification.revoke(entity.owner, entity.feedbackId);
        feedback.delete(entity);
    }

    private boolean equivalent(Feedback entity, Request request) {
        return entity.transportType == request.transportType()
            && Objects.equals(entity.line, request.line().trim())
            && entity.punctualityScore == request.punctualityScore()
            && entity.cleanlinessScore == request.cleanlinessScore()
            && entity.crowdingScore == request.crowdingScore()
            && Objects.equals(entity.comment, normalizeComment(request.comment()))
            && Double.compare(entity.latitude, request.latitude()) == 0
            && Double.compare(entity.longitude, request.longitude()) == 0
            && entity.createdAt == request.createdAt();
    }

    private String normalizeComment(String comment) {
        if (comment == null) return null;
        String trimmed = comment.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private double overall(int punctuality, int cleanliness, int crowding) {
        double average = (punctuality + cleanliness + crowding) / 3.0;
        return Math.round(average * 10.0) / 10.0;
    }

    private Response out(Feedback entity) {
        return out(entity,0,List.of());
    }
    private Response out(Feedback entity,int xpAwarded,List<String> newAchievements) {
        return new Response(
            entity.feedbackId,
            entity.feedbackId.toString(),
            entity.owner.getId(),
            entity.owner.getUsername(),
            entity.owner.getAvatarKey(),
            entity.transportType,
            entity.line,
            entity.score,
            entity.score,
            entity.punctualityScore,
            entity.cleanlinessScore,
            entity.crowdingScore,
            entity.comment,
            entity.latitude,
            entity.longitude,
            entity.createdAt,
            xpAwarded,
            newAchievements
        );
    }
}

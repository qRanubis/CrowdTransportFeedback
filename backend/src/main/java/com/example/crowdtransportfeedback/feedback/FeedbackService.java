package com.example.crowdtransportfeedback.feedback;

import com.example.crowdtransportfeedback.common.ApiException;
import com.example.crowdtransportfeedback.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import static com.example.crowdtransportfeedback.feedback.FeedbackDtos.Request;
import static com.example.crowdtransportfeedback.feedback.FeedbackDtos.Response;

@Service
public class FeedbackService {
    private final FeedbackRepository feedback;
    private final UserRepository users;

    FeedbackService(FeedbackRepository feedback, UserRepository users) {
        this.feedback = feedback;
        this.users = users;
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
            if (!existing.owner.id.equals(userId) || !equivalent(existing, request)) {
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
        entity.score = request.score();
        entity.punctualityScore = request.punctualityScore();
        entity.cleanlinessScore = request.cleanlinessScore();
        entity.crowdingScore = request.crowdingScore();
        entity.comment = normalizeComment(request.comment());
        entity.latitude = request.latitude();
        entity.longitude = request.longitude();
        entity.createdAt = request.createdAt();
        return out(feedback.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        if (!feedback.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "feedback_not_found", "Feedback was not found");
        }
        feedback.deleteById(id);
    }

    private boolean equivalent(Feedback entity, Request request) {
        return entity.transportType == request.transportType()
            && Objects.equals(entity.line, request.line().trim())
            && entity.score == request.score()
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

    private Response out(Feedback entity) {
        return new Response(
            entity.feedbackId,
            entity.feedbackId.toString(),
            entity.owner.id,
            entity.transportType,
            entity.line,
            entity.score,
            entity.punctualityScore,
            entity.cleanlinessScore,
            entity.crowdingScore,
            entity.comment,
            entity.latitude,
            entity.longitude,
            entity.createdAt
        );
    }
}

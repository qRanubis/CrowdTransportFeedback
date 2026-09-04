package com.example.crowdtransportfeedback.feedback;

import com.example.crowdtransportfeedback.security.JwtService.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import static com.example.crowdtransportfeedback.feedback.FeedbackDtos.*;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
    private final FeedbackService service;

    FeedbackController(FeedbackService service) {
        this.service = service;
    }

    @GetMapping
    List<Response> all() {
        return service.all();
    }

    @GetMapping("/{id}")
    Response get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    ResponseEntity<Response> create(
        @Valid @RequestBody Request request,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.status(201).body(service.create(request, user.id()));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(
        @PathVariable UUID id,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        service.delete(id, user.id(), user.role());
        return ResponseEntity.noContent().build();
    }
}

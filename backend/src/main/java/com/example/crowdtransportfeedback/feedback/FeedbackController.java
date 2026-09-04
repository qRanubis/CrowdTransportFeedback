package com.example.crowdtransportfeedback.feedback;
import com.example.crowdtransportfeedback.security.JwtService.AuthenticatedUser; import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*; import java.util.*; import static com.example.crowdtransportfeedback.feedback.FeedbackDtos.*;
@RestController @RequestMapping("/api/feedback") public class FeedbackController {
 private final FeedbackService service; FeedbackController(FeedbackService s){service=s;}
 @GetMapping List<Response> all(){return service.all();} @GetMapping("/{id}") Response get(@PathVariable UUID id){return service.get(id);}
 @PostMapping ResponseEntity<Response> create(@Valid @RequestBody Request r,@AuthenticationPrincipal AuthenticatedUser u){return ResponseEntity.status(201).body(service.create(r,u.id()));}
 @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") ResponseEntity<Void> delete(@PathVariable UUID id){service.delete(id);return ResponseEntity.noContent().build();}
}

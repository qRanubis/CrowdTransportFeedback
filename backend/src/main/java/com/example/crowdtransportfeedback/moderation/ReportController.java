package com.example.crowdtransportfeedback.moderation;
import com.example.crowdtransportfeedback.security.JwtService.AuthenticatedUser; import jakarta.validation.Valid; import java.util.UUID;
import org.springframework.http.ResponseEntity; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/feedback/{feedbackId}/reports") public class ReportController {
 private final ReportService service; public ReportController(ReportService s){service=s;}
 @PostMapping public ResponseEntity<ReportDtos.Created> create(@PathVariable UUID feedbackId,@Valid @RequestBody ReportDtos.CreateRequest request,@AuthenticationPrincipal AuthenticatedUser user){return ResponseEntity.status(201).body(service.create(feedbackId,user.id(),request));}
 @GetMapping("/me") public ReportDtos.Mine mine(@PathVariable UUID feedbackId,@AuthenticationPrincipal AuthenticatedUser user){return service.mine(feedbackId,user.id());}
}

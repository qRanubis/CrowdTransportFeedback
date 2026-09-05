package com.example.crowdtransportfeedback.admin;
import com.example.crowdtransportfeedback.security.JwtService.AuthenticatedUser; import java.util.UUID; import org.springframework.http.*; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin") public class AdminController {
 private final AdminService service; public AdminController(AdminService s){service=s;}
 @GetMapping("/overview") AdminDtos.Overview overview(){return service.overview();}
 @GetMapping("/moderation/reports") AdminDtos.Page<AdminDtos.QueueItem> reports(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return service.queue(page,size);}
 @GetMapping("/moderation/feedback/{id}") AdminDtos.ModerationDetail detail(@PathVariable UUID id){return service.detail(id);}
 @PostMapping("/moderation/feedback/{id}/resolve") ResponseEntity<Void> resolve(@PathVariable UUID id,@RequestBody AdminDtos.ResolveRequest request,@AuthenticationPrincipal AuthenticatedUser user){service.resolve(id,user.id(),request);return ResponseEntity.noContent().build();}
 @GetMapping("/feedback") AdminDtos.Page<AdminDtos.AdminFeedback> feedback(@RequestParam(required=false)String transportType,@RequestParam(required=false)String line,@RequestParam(defaultValue="ALL")String window,@RequestParam(required=false)String username,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return service.feedbackList(transportType,line,window,username,page,size);}
 @GetMapping("/users") AdminDtos.Page<AdminDtos.AdminUser> users(@RequestParam(required=false)String query,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return service.users(query,page,size);}
 @GetMapping("/reporting/summary") AdminDtos.ReportingSummary summary(@RequestParam(defaultValue="ALL")String window,@RequestParam(required=false)String transportType,@RequestParam(required=false)String line){return service.summary(window,transportType,line);}
 @GetMapping(value="/reporting/feedback.csv",produces="text/csv") ResponseEntity<byte[]> csv(@RequestParam(defaultValue="ALL")String window,@RequestParam(required=false)String transportType,@RequestParam(required=false)String line){return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=feedback-export.csv").contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(service.csv(window,transportType,line));}
}

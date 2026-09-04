package com.example.crowdtransportfeedback.auth;
import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import static com.example.crowdtransportfeedback.auth.AuthDtos.*;
@RestController @RequestMapping("/api/auth") public class AuthController {
 private final AuthService auth; AuthController(AuthService a){auth=a;}
 @PostMapping("/register") ResponseEntity<AuthResponse> register(@Valid @RequestBody Credentials c){return ResponseEntity.status(201).body(auth.register(c));}
 @PostMapping("/login") AuthResponse login(@Valid @RequestBody Credentials c){return auth.login(c);}
 @PostMapping("/refresh") AuthResponse refresh(@Valid @RequestBody RefreshRequest r){return auth.refresh(r.refreshToken());}
 @PostMapping("/logout") ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest r){auth.logout(r.refreshToken());return ResponseEntity.noContent().build();}
}

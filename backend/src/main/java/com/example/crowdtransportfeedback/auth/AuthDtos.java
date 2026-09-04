package com.example.crowdtransportfeedback.auth;
import com.example.crowdtransportfeedback.user.*; import jakarta.validation.constraints.*; import java.util.UUID;
public final class AuthDtos {
 private AuthDtos(){} public record Credentials(@NotBlank @Email String email,@NotBlank @Size(min=8,max=128) String password){}
 public record RefreshRequest(@NotBlank String refreshToken){} public record UserSummary(UUID id,String email,Role role){}
 public record AuthResponse(String accessToken,String refreshToken,UserSummary user){}
}

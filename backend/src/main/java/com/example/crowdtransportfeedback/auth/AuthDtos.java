package com.example.crowdtransportfeedback.auth;

import com.example.crowdtransportfeedback.user.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class AuthDtos {
    private static final String LOGIN_EMAIL_PATTERN =
        "^\\s*[^\\s@]+@[^\\s@]+\\.[^\\s@]+\\s*$";
    private static final String REGISTRATION_EMAIL_PATTERN =
        "^\\s*[^\\s@]+@[^\\s@]+\\.[A-Za-z]{2,3}\\s*$";
    private static final String USERNAME_PATTERN = "^[a-z0-9]{3,20}$";
    private static final String PASSWORD_PATTERN =
        "^(?=.{8,128}$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).*$";

    private AuthDtos() {}

    public record Credentials(
        @NotBlank
        @Size(max = 324)
        @Pattern(regexp = LOGIN_EMAIL_PATTERN, message = "must be a valid email address")
        String email,
        @NotBlank @Size(max = 128) String password
    ) {}

    public record RegisterRequest(
        @NotBlank
        @Size(max = 324)
        @Pattern(
            regexp = REGISTRATION_EMAIL_PATTERN,
            message = "must end with a 2-3 letter top-level domain"
        )
        String email,
        @NotBlank
        @Pattern(
            regexp = USERNAME_PATTERN,
            message = "must contain only lowercase letters and digits and be 3-20 characters"
        )
        String username,
        @NotBlank
        @Size(min = 8, max = 128)
        @Pattern(
            regexp = PASSWORD_PATTERN,
            message = "must include lowercase, uppercase, digit and symbol"
        )
        String password
    ) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record UserSummary(UUID id, String email, String username, Role role) {}

    public record AuthResponse(String accessToken, String refreshToken, UserSummary user) {}
}

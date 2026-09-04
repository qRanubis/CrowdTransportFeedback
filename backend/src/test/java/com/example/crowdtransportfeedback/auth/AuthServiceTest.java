package com.example.crowdtransportfeedback.auth;

import com.example.crowdtransportfeedback.common.ApiException;
import com.example.crowdtransportfeedback.security.JwtProperties;
import com.example.crowdtransportfeedback.security.JwtService;
import com.example.crowdtransportfeedback.user.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    UserRepository users = mock(UserRepository.class);
    RefreshSessionRepository sessions = mock(RefreshSessionRepository.class);
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    AuthService service;

    @BeforeEach
    void setup() {
        service = new AuthService(
            users,
            sessions,
            encoder,
            new JwtService(new JwtProperties("a-secret-with-at-least-thirty-two-bytes-long", Duration.ofMinutes(5))),
            Duration.ofDays(90)
        );
        when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void registrationNormalizesEmailStoresUsernameAndHashesPassword() {
        var response = service.register(
            new AuthDtos.RegisterRequest(" USER@Example.com ", "user123", "Password123!")
        );
        var captor = ArgumentCaptor.forClass(AppUser.class);
        verify(users).save(captor.capture());
        assertEquals(Role.USER, captor.getValue().role);
        assertEquals("user@example.com", captor.getValue().email);
        assertEquals("user123", captor.getValue().username);
        assertNotEquals("Password123!", captor.getValue().passwordHash);
        assertTrue(encoder.matches("Password123!", captor.getValue().passwordHash));
        assertEquals("user123", response.user().username());
    }

    @Test
    void duplicateEmailIsRejected() {
        when(users.existsByEmail("user@example.com")).thenReturn(true);
        assertEquals(
            "email_exists",
            assertThrows(ApiException.class, () -> service.register(
                new AuthDtos.RegisterRequest("USER@example.com", "user123", "Password123!")
            )).code
        );
    }

    @Test
    void duplicateUsernameIsRejected() {
        when(users.existsByUsername("user123")).thenReturn(true);
        assertEquals(
            "username_exists",
            assertThrows(ApiException.class, () -> service.register(
                new AuthDtos.RegisterRequest("user@example.com", "user123", "Password123!")
            )).code
        );
    }

    @Test
    void validLoginSucceedsAndInvalidPasswordDoesNotRevealAccount() {
        var user = new AppUser(
            UUID.randomUUID(), "user@example.com", "user123",
            encoder.encode("legacy-password"), Role.USER, Instant.now()
        );
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        assertNotNull(service.login(
            new AuthDtos.Credentials("user@example.com", "legacy-password")
        ).accessToken());
        assertEquals(
            "invalid_credentials",
            assertThrows(ApiException.class, () -> service.login(
                new AuthDtos.Credentials("user@example.com", "wrong-pass")
            )).code
        );
    }

    @Test
    void refreshRotatesAndOldTokenCannotBeReused() {
        var user = new AppUser(
            UUID.randomUUID(), "user@example.com", "user123",
            encoder.encode("legacy-password"), Role.USER, Instant.now()
        );
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        var first = service.login(new AuthDtos.Credentials("user@example.com", "legacy-password"));
        var captor = ArgumentCaptor.forClass(RefreshSession.class);
        verify(sessions).save(captor.capture());
        var old = captor.getValue();
        when(sessions.findByTokenHash(AuthService.hash(first.refreshToken()))).thenReturn(Optional.of(old));
        var rotated = service.refresh(first.refreshToken());
        assertNotEquals(first.refreshToken(), rotated.refreshToken());
        assertNotNull(old.revokedAt);
        assertThrows(ApiException.class, () -> service.refresh(first.refreshToken()));
    }

    @Test
    void logoutRevokesSessionAndExpiredSessionIsRejected() {
        var user = new AppUser(UUID.randomUUID(), "user@example.com", "user123", "hash", Role.USER, Instant.now());
        var active = new RefreshSession(UUID.randomUUID(), user, AuthService.hash("logout-token"), Instant.now(), Instant.now().plusSeconds(60));
        when(sessions.findByTokenHash(active.tokenHash)).thenReturn(Optional.of(active));
        service.logout("logout-token");
        assertNotNull(active.revokedAt);
        var expired = new RefreshSession(UUID.randomUUID(), user, AuthService.hash("expired-token"), Instant.now().minusSeconds(120), Instant.now().minusSeconds(1));
        when(sessions.findByTokenHash(expired.tokenHash)).thenReturn(Optional.of(expired));
        assertThrows(ApiException.class, () -> service.refresh("expired-token"));
    }
}

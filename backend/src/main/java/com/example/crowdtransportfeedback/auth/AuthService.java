package com.example.crowdtransportfeedback.auth;

import com.example.crowdtransportfeedback.common.ApiException;
import com.example.crowdtransportfeedback.security.JwtService;
import com.example.crowdtransportfeedback.user.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import static com.example.crowdtransportfeedback.auth.AuthDtos.*;

@Service
public class AuthService {
    private final UserRepository users;
    private final RefreshSessionRepository sessions;
    private final PasswordEncoder passwords;
    private final JwtService jwt;
    private final Duration inactivity;
    private final SecureRandom random = new SecureRandom();

    AuthService(
        UserRepository users,
        RefreshSessionRepository sessions,
        PasswordEncoder passwords,
        JwtService jwt,
        @Value("${app.refresh-inactivity:P90D}") Duration inactivity
    ) {
        this.users = users;
        this.sessions = sessions;
        this.passwords = passwords;
        this.jwt = jwt;
        this.inactivity = inactivity;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        String username = request.username();

        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "email_exists", "An account already exists for this email");
        }
        if (users.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "username_exists", "This username is already in use");
        }

        AppUser user = users.save(
            new AppUser(
                UUID.randomUUID(),
                email,
                username,
                passwords.encode(request.password()),
                Role.USER,
                Instant.now()
            )
        );
        return create(user);
    }

    @Transactional
    public AuthResponse login(Credentials credentials) {
        AppUser user = users.findByEmail(normalize(credentials.email()))
            .filter(candidate -> passwords.matches(credentials.password(), candidate.passwordHash))
            .orElseThrow(() -> new ApiException(
                HttpStatus.UNAUTHORIZED,
                "invalid_credentials",
                "Invalid email or password"
            ));
        return create(user);
    }

    @Transactional
    public AuthResponse refresh(String raw) {
        RefreshSession old = sessions.findByTokenHash(hash(raw)).orElseThrow(AuthService::invalid);
        Instant now = Instant.now();
        if (old.revokedAt != null || !old.expiresAt.isAfter(now)) throw invalid();

        old.revokedAt = now;
        old.lastUsedAt = now;
        var pair = newToken(old.user, now);
        old.replacedBy = pair.session.id;
        sessions.save(old);
        sessions.save(pair.session);
        return response(old.user, pair.raw);
    }

    @Transactional
    public void logout(String raw) {
        sessions.findByTokenHash(hash(raw)).ifPresent(session -> {
            if (session.revokedAt == null) session.revokedAt = Instant.now();
        });
    }

    private AuthResponse create(AppUser user) {
        var pair = newToken(user, Instant.now());
        sessions.save(pair.session);
        return response(user, pair.raw);
    }

    private Pair newToken(AppUser user, Instant now) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new Pair(
            raw,
            new RefreshSession(UUID.randomUUID(), user, hash(raw), now, now.plus(inactivity))
        );
    }

    private AuthResponse response(AppUser user, String refresh) {
        return new AuthResponse(
            jwt.issue(user),
            refresh,
            new UserSummary(user.id, user.email, user.getUsername(), user.role)
        );
    }

    static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    static ApiException invalid() {
        return new ApiException(
            HttpStatus.UNAUTHORIZED,
            "invalid_refresh",
            "Refresh session is invalid or expired"
        );
    }

    static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    private record Pair(String raw, RefreshSession session) {}
}

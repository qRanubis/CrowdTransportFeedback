package com.example.crowdtransportfeedback.user;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9]{3,20}$");
    private static final Pattern STRONG_PASSWORD = Pattern.compile(
        "^(?=.{8,128}$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).*$"
    );

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String username;
    private final String password;

    AdminBootstrap(
        UserRepository users,
        PasswordEncoder encoder,
        @Value("${app.admin.email:}") String email,
        @Value("${app.admin.username:admin}") String username,
        @Value("${app.admin.password:}") String password
    ) {
        this.users = users;
        this.encoder = encoder;
        this.email = email;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) return;

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String normalizedUsername = username.trim();

        if (!USERNAME.matcher(normalizedUsername).matches()) {
            throw new IllegalStateException(
                "APP_ADMIN_USERNAME must contain only lowercase letters and digits and be 3-20 characters"
            );
        }
        if (!STRONG_PASSWORD.matcher(password).matches()) {
            throw new IllegalStateException(
                "APP_ADMIN_PASSWORD must be 8-128 characters and include lowercase, uppercase, digit and symbol"
            );
        }

        if (users.existsByEmail(normalizedEmail)) return;
        if (users.existsByUsername(normalizedUsername)) {
            throw new IllegalStateException("APP_ADMIN_USERNAME is already in use");
        }

        users.save(
            new AppUser(
                UUID.randomUUID(),
                normalizedEmail,
                normalizedUsername,
                encoder.encode(password),
                Role.ADMIN,
                Instant.now()
            )
        );
    }
}

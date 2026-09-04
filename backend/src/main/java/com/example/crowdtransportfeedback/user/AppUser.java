package com.example.crowdtransportfeedback.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUser {
    @Id
    public UUID id;

    @Column(nullable = false, unique = true, length = 320)
    public String email;

    @Column(nullable = false, unique = true, length = 20)
    public String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    public String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public Role role;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "avatar_key", nullable = false, length = 24)
    public String avatarKey = "COMMUTER";

    protected AppUser() {}

    public AppUser(UUID id, String email, String username, String hash, Role role, Instant at) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.passwordHash = hash;
        this.role = role;
        this.createdAt = at;
    }

    public AppUser(UUID id, String email, String hash, Role role, Instant at) {
        this(id, email, generatedUsername(id), hash, role, at);
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    private static String generatedUsername(UUID id) {
        return "user" + id.toString().replace("-", "").substring(0, 16);
    }
}

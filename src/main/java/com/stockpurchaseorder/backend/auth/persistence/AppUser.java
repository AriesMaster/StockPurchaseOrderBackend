package com.stockpurchaseorder.backend.auth.persistence;

import com.stockpurchaseorder.backend.auth.domain.Role;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "app_user")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "text")
    private String email;

    @Column(name = "password_hash", columnDefinition = "text")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private Role role;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AppUser() {}

    public AppUser(String email, Role role) {
        this.email = normalizeEmail(email);
        this.role = Objects.requireNonNull(role);
        this.createdAt = Instant.now();
        this.enabled = false;
    }

    public static String normalizeEmail(String email) {
        String normalized = Objects.requireNonNull(email).strip().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) throw new IllegalArgumentException("Email is required");
        return normalized;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
}

package com.stockpurchaseorder.backend.auth.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import java.util.Objects;

@Entity
@Table(name = "auth_session")
public class AuthSession {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;
    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected AuthSession() {}

    public AuthSession(Long userId, Instant now) {
        this.id = UUID.randomUUID();
        this.userId = Objects.requireNonNull(userId);
        this.createdAt = Objects.requireNonNull(now);
        this.expiresAt = now.plusSeconds(7 * 86400L);
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && !now.isBefore(createdAt) && now.isBefore(expiresAt);
    }

    public void revoke(Instant now) {
        if (revokedAt == null) revokedAt = Objects.requireNonNull(now);
    }

    public UUID getId() { return id; }
    public Long getUserId() { return userId; }
    public Instant getExpiresAt() { return expiresAt; }
}

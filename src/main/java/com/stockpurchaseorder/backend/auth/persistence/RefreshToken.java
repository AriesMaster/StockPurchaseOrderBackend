package com.stockpurchaseorder.backend.auth.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import java.util.Objects;

@Entity
@Table(name = "refresh_token")
public class RefreshToken {
    @Id
    @Column(name = "token_digest", length = 64)
    private String tokenDigest;
    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;
    @Column(name = "consumed_at")
    private Instant consumedAt;

    protected RefreshToken() {}

    public RefreshToken(String digest, UUID sessionId, Instant now, Instant expiresAt) {
        if (digest == null || !digest.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid digest");
        if (!expiresAt.isAfter(now)) throw new IllegalArgumentException("Invalid expiry");
        this.tokenDigest = digest;
        this.sessionId = Objects.requireNonNull(sessionId);
        this.createdAt = now;
        this.expiresAt = expiresAt;
    }

    public boolean isConsumed() { return consumedAt != null; }
    public boolean isActive(Instant now) {
        return !isConsumed() && !now.isBefore(createdAt) && now.isBefore(expiresAt);
    }
    public void consume(Instant now) {
        if (!isActive(now)) throw new IllegalStateException("Token is not active");
        consumedAt = now;
    }
    public UUID getSessionId() { return sessionId; }
    public String getTokenDigest() { return tokenDigest; }
}

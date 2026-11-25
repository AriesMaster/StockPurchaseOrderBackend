package com.stockpurchaseorder.backend.auth.service;

import com.stockpurchaseorder.backend.auth.persistence.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class SessionPersistenceTest {
    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");
    private final RefreshTokenService tokens = new RefreshTokenService();

    @Test
    void sessionExpiresAfterSevenDays() {
        var session = new AuthSession(1L, now);
        assertTrue(session.isActive(now));
        assertEquals(now.plusSeconds(604800), session.getExpiresAt());
        assertFalse(session.isActive(session.getExpiresAt()));
    }

    @Test
    void revokedSessionIsInactive() {
        var session = new AuthSession(1L, now);
        session.revoke(now);
        assertFalse(session.isActive(now));
    }

    @Test
    void refreshTokensAreRandomAndDigestsAreDeterministic() {
        String token = tokens.generate();
        assertEquals(43, token.length());
        assertNotEquals(token, tokens.generate());
        assertEquals(64, tokens.digest(token).length());
        assertEquals(tokens.digest(token), tokens.digest(token));
        assertNotEquals(token, tokens.digest(token));
    }

    @Test
    void consumedTokenCannotBeUsedAgain() {
        var session = new AuthSession(1L, now);
        var token = new RefreshToken(tokens.digest(tokens.generate()), session.getId(), now, session.getExpiresAt());
        assertTrue(token.isActive(now));
        token.consume(now);
        assertTrue(token.isConsumed());
        assertFalse(token.isActive(now));
        assertThrows(IllegalStateException.class, () -> token.consume(now));
    }

    @Test
    void expiredRefreshTokenIsInactive() {
        var session = new AuthSession(1L, now);
        var token = new RefreshToken(tokens.digest(tokens.generate()), session.getId(), now, session.getExpiresAt());
        assertFalse(token.isActive(session.getExpiresAt()));
    }
}

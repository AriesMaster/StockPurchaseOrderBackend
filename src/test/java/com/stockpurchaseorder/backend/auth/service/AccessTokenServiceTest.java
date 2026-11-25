package com.stockpurchaseorder.backend.auth.service;

import com.stockpurchaseorder.backend.auth.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;
import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AccessTokenServiceTest {
    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");
    private final byte[] key = new byte[32];
    private final AccessTokenService service = new AccessTokenService(key, Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void roundTripPreservesIdentityAndLifetime() {
        UUID session = UUID.randomUUID();
        var jwt = service.decode(service.issue(1, session, Role.ADMIN));
        assertEquals("1", jwt.getSubject());
        assertEquals(session.toString(), jwt.getClaimAsString("sid"));
        assertEquals("ADMIN", jwt.getClaimAsString("role"));
        assertEquals(now.plusSeconds(900), jwt.getExpiresAt());
        assertTrue(jwt.getAudience().contains("stockflow-api"));
        assertEquals("stockflow", jwt.getIssuer().toString());
    }

    @Test
    void rejectsExpiredToken() {
        String token = service.issue(1, UUID.randomUUID(), Role.ADMIN);
        var later = new AccessTokenService(key, Clock.fixed(now.plusSeconds(901), ZoneOffset.UTC));
        assertThrows(JwtException.class, () -> later.decode(token));
    }

    @Test
    void rejectsDifferentSigningKey() {
        byte[] other = new byte[32];
        other[0] = 1;
        var verifier = new AccessTokenService(other, Clock.fixed(now, ZoneOffset.UTC));
        String token = service.issue(1, UUID.randomUUID(), Role.ADMIN);
        assertThrows(JwtException.class, () -> verifier.decode(token));
    }

    @Test
    void rejectsShortSigningKey() {
        assertThrows(IllegalArgumentException.class, () -> new AccessTokenService(new byte[16], Clock.systemUTC()));
    }
}

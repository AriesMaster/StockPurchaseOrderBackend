package com.stockpurchaseorder.backend.auth.service;

import com.stockpurchaseorder.backend.auth.domain.Role;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import javax.crypto.spec.SecretKeySpec;
import java.time.*;
import java.util.List;
import java.util.UUID;

public final class AccessTokenService {
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Clock clock;
    private static final String ISSUER = "stockflow";
    private static final String AUDIENCE = "stockflow-api";

    public AccessTokenService(byte[] secret, Clock clock) {
        if (secret.length < 32) throw new IllegalArgumentException("JWT key must contain at least 32 bytes");
        this.clock = clock;
        var key = new SecretKeySpec(secret, "HmacSHA256");
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        var verifier = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        var timestamps = new JwtTimestampValidator(Duration.ZERO);
        timestamps.setClock(clock);
        verifier.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                timestamps, new JwtIssuerValidator(ISSUER),
                jwt -> jwt.getAudience() != null && jwt.getAudience().contains(AUDIENCE)
                        && jwt.getExpiresAt() != null && validIdentity(jwt)
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"))));
        decoder = verifier;
    }

    private static boolean validIdentity(Jwt jwt) {
        try {
            return Long.parseLong(jwt.getSubject()) > 0
                    && UUID.fromString(jwt.getClaimAsString("sid")) != null
                    && Role.valueOf(jwt.getClaimAsString("role")) != null;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public String issue(long userId, UUID sessionId, Role role) {
        if (userId <= 0) throw new IllegalArgumentException("Invalid user ID");
        Instant now = clock.instant();
        var claims = JwtClaimsSet.builder().issuer(ISSUER).audience(List.of(AUDIENCE))
                .subject(Long.toString(userId)).issuedAt(now).expiresAt(now.plus(Duration.ofMinutes(15)))
                .id(UUID.randomUUID().toString()).claim("sid", sessionId.toString())
                .claim("role", role.name()).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    public Jwt decode(String token) {
        return decoder.decode(token);
    }
}

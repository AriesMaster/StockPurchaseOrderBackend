package com.stockpurchaseorder.backend.auth.service;

import com.stockpurchaseorder.backend.auth.persistence.AppUserRepository;
import com.stockpurchaseorder.backend.auth.persistence.AuthSessionRepository;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Component
public class SessionJwtDecoder implements JwtDecoder {
    private final AccessTokenService access;
    private final AuthSessionRepository sessions;
    private final AppUserRepository users;

    public SessionJwtDecoder(AccessTokenService access, AuthSessionRepository sessions, AppUserRepository users) {
        this.access = access;
        this.sessions = sessions;
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public Jwt decode(String token) {
        Jwt jwt = access.decode(token);
        long id = Long.parseLong(jwt.getSubject());
        UUID sessionId = UUID.fromString(jwt.getClaimAsString("sid"));
        sessions.findById(sessionId)
                .filter(s -> s.getUserId().equals(id) && s.isActive(Instant.now()))
                .orElseThrow(() -> new BadJwtException("Invalid session"));
        users.findById(id)
                .filter(u -> u.isEnabled() && u.getRole().name().equals(jwt.getClaimAsString("role")))
                .orElseThrow(() -> new BadJwtException("Invalid account"));
        return jwt;
    }
}

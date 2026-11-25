package com.stockpurchaseorder.backend.auth.service;

import com.stockpurchaseorder.backend.auth.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import java.util.Optional;

@Service
public class SessionService {
    private final AuthSessionRepository sessions;
    private final RefreshTokenRepository tokens;
    private final AppUserRepository users;
    private final RefreshTokenService generator;
    private final AccessTokenService access;
    private final EntityManager em;

    public SessionService(AuthSessionRepository sessions, RefreshTokenRepository tokens,
            AppUserRepository users, RefreshTokenService generator, AccessTokenService access,
            EntityManager em) {
        this.sessions = sessions;
        this.tokens = tokens;
        this.users = users;
        this.generator = generator;
        this.access = access;
        this.em = em;
    }

    public record Credentials(String accessToken, String refreshToken, UUID sessionId) {
        @Override public String toString() { return "Credentials[redacted]"; }
    }

    // Caller must authenticate credentials before invoking this method.
    @Transactional
    public Optional<Credentials> create(long userId) {
        var user = users.findById(userId).filter(AppUser::isEnabled);
        if (user.isEmpty()) return Optional.empty();
        Instant now = Instant.now();
        var session = new AuthSession(userId, now);
        em.persist(session);
        em.flush();
        return Optional.of(issue(session, user.get(), now));
    }

    @Transactional
    public Optional<Credentials> rotate(String rawToken) {
        String digest;
        try { digest = generator.digest(rawToken); }
        catch (IllegalArgumentException ex) { return Optional.empty(); }
        var initial = tokens.findById(digest);
        if (initial.isEmpty()) return Optional.empty();
        UUID sessionId = initial.get().getSessionId();
        var locked = sessions.findForUpdate(sessionId);
        if (locked.isEmpty()) return Optional.empty();
        var session = locked.get();
        var token = initial.get();
        // Reload after acquiring the session lock to avoid stale persistence-context state.
        em.refresh(token);
        Instant now = Instant.now();
        if (!session.isActive(now)) return Optional.empty();
        if (token.isConsumed()) {
            session.revoke(now);
            return Optional.empty();
        }
        if (!token.isActive(now)) return Optional.empty();
        var user = users.findById(session.getUserId()).filter(AppUser::isEnabled);
        if (user.isEmpty()) {
            session.revoke(now);
            return Optional.empty();
        }
        token.consume(now);
        return Optional.of(issue(session, user.get(), now));
    }

    @Transactional
    public void revoke(UUID sessionId, long userId) {
        sessions.findForUpdate(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .ifPresent(s -> s.revoke(Instant.now()));
    }

    private Credentials issue(AuthSession session, AppUser user, Instant now) {
        String raw = generator.generate();
        em.persist(new RefreshToken(generator.digest(raw), session.getId(), now, session.getExpiresAt()));
        return new Credentials(access.issue(user.getId(), session.getId(), user.getRole()), raw, session.getId());
    }
}

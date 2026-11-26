package com.stockpurchaseorder.backend.auth.service;

import com.stockpurchaseorder.backend.auth.dto.*;
import com.stockpurchaseorder.backend.auth.persistence.AppUser;
import com.stockpurchaseorder.backend.auth.persistence.AppUserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
    private final AuthenticationManager manager;
    private final AppUserRepository users;
    private final SessionService sessions;
    private final AccessTokenService access;

    public AuthenticationService(AuthenticationManager manager, AppUserRepository users,
            SessionService sessions, AccessTokenService access) {
        this.manager = manager;
        this.users = users;
        this.sessions = sessions;
        this.access = access;
    }

    public record Result(AuthResponse response, String refreshToken) {
        @Override public String toString() { return "Result[redacted]"; }
    }

    public Result login(LoginRequest request) {
        String email;
        try {
            email = AppUser.normalizeEmail(request.email());
            manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
        } catch (AuthenticationException | IllegalArgumentException ex) {
            throw invalidCredentials();
        }
        var user = users.findByEmail(email).filter(AppUser::isEnabled)
                .orElseThrow(AuthenticationService::invalidCredentials);
        return response(sessions.create(user.getId()).orElseThrow(AuthenticationService::invalidCredentials));
    }

    public Result refresh(String token) {
        return response(sessions.rotate(token).orElseThrow(AuthenticationService::invalidCredentials));
    }

    private Result response(SessionService.Credentials credentials) {
        var jwt = access.decode(credentials.accessToken());
        var user = users.findById(Long.parseLong(jwt.getSubject())).filter(AppUser::isEnabled)
                .orElseThrow(AuthenticationService::invalidCredentials);
        return new Result(new AuthResponse(credentials.accessToken(),
                new AuthUser(user.getId(), user.getEmail(), user.getRole())), credentials.refreshToken());
    }

    private static BadCredentialsException invalidCredentials() {
        return new BadCredentialsException("Invalid credentials");
    }
}

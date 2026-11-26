package com.stockpurchaseorder.backend.auth.controller;

import com.stockpurchaseorder.backend.auth.dto.AuthResponse;
import com.stockpurchaseorder.backend.auth.dto.LoginRequest;
import com.stockpurchaseorder.backend.auth.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationService authentication;
    private final com.stockpurchaseorder.backend.auth.service.SessionService sessions;
    private static final String COOKIE = "__Secure-stockflow-refresh";

    public AuthController(AuthenticationService authentication, com.stockpurchaseorder.backend.auth.service.SessionService sessions) {
        this.authentication = authentication;
        this.sessions = sessions;
    }

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken token) {
        token.getToken();
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return credentials(authentication.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = COOKIE, required = false) String token) {
        return credentials(authentication.refresh(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = COOKIE, required = false) String token) {
        sessions.revokeByRefreshToken(token);
        var cookie = ResponseCookie.from(COOKIE, "").httpOnly(true).secure(true)
                .sameSite("Strict").path("/api/auth").maxAge(0).build();
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    @GetMapping("/me")
    public ResponseEntity<com.stockpurchaseorder.backend.auth.dto.AuthUser> me(
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(authentication.currentUser(Long.parseLong(jwt.getSubject())));
    }

    private ResponseEntity<AuthResponse> credentials(AuthenticationService.Result result) {
        var cookie = ResponseCookie.from(COOKIE, result.refreshToken())
                .httpOnly(true).secure(true).sameSite("Strict").path("/api/auth").build();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, cookie.toString()).body(result.response());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<java.util.Map<String, String>> invalidCredentials() {
        var cookie = ResponseCookie.from(COOKIE, "").httpOnly(true).secure(true)
                .sameSite("Strict").path("/api/auth").maxAge(0).build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(java.util.Map.of("message", "Invalid credentials"));
    }
}

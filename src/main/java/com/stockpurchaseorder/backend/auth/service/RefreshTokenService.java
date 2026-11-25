package com.stockpurchaseorder.backend.auth.service;

import org.springframework.stereotype.Service;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String digest(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}

package com.stockpurchaseorder.backend.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderTest {
    private final PasswordEncoder encoder = new SecurityConfig().passwordEncoder();

    @Test
    void hashesAndVerifiesPasswordUsingArgon2id() {
        String password = "example-test-password";
        String hash = encoder.encode(password);
        assertTrue(hash.startsWith("$argon2id$"));
        assertTrue(encoder.matches(password, hash));
        assertFalse(encoder.matches("wrong-password", hash));
        assertNotEquals(password, hash);
    }

    @Test
    void generatesDifferentHashesForTheSamePassword() {
        String password = "example-test-password";
        assertNotEquals(encoder.encode(password), encoder.encode(password));
    }
}

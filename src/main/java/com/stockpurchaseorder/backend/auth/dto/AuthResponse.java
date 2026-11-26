package com.stockpurchaseorder.backend.auth.dto;

public record AuthResponse(String accessToken, AuthUser user) {
    @Override public String toString() { return "AuthResponse[redacted]"; }
}

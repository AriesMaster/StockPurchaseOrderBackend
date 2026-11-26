package com.stockpurchaseorder.backend.auth.dto;

import com.stockpurchaseorder.backend.auth.domain.Role;
import com.stockpurchaseorder.backend.auth.persistence.AppUser;
import java.time.Instant;

public record EmployeeResponse(Long id, String email, Role role, boolean enabled, Instant createdAt) {
    public static EmployeeResponse from(AppUser user) {
        return new EmployeeResponse(user.getId(), user.getEmail(), user.getRole(), user.isEnabled(), user.getCreatedAt());
    }
}

package com.stockpurchaseorder.backend.auth.dto;

import com.stockpurchaseorder.backend.auth.domain.Role;
import jakarta.validation.constraints.*;

public record CreateEmployeeRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull Role role,
        @NotBlank @Size(min = 15, max = 128) String password) {
    @Override public String toString() { return "CreateEmployeeRequest[redacted]"; }
}

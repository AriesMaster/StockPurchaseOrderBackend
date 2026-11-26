package com.stockpurchaseorder.backend.auth.dto;

import com.stockpurchaseorder.backend.auth.domain.Role;

public record AuthUser(long id, String email, Role role) {}

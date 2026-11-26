package com.stockpurchaseorder.backend.auth.service;

import com.stockpurchaseorder.backend.auth.domain.Role;
import com.stockpurchaseorder.backend.auth.dto.*;
import com.stockpurchaseorder.backend.auth.persistence.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmployeeService {
    private final AppUserRepository users;
    private final PasswordEncoder passwords;
    private final AuthSessionRepository sessions;

    public EmployeeService(AppUserRepository users, PasswordEncoder passwords, AuthSessionRepository sessions) {
        this.users = users;
        this.passwords = passwords;
        this.sessions = sessions;
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> list(int page, int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination");
        return users.findAll(PageRequest.of(page, size, Sort.by("id"))).map(EmployeeResponse::from);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse get(long id) {
        return users.findById(id).map(EmployeeResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request) {
        if (request.role() == Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Additional administrator provisioning is not supported");
        String email = AppUser.normalizeEmail(request.email());
        if (users.existsByEmail(email))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        var user = new AppUser(email, request.role());
        user.activate(passwords.encode(request.password()));
        try {
            return EmployeeResponse.from(users.saveAndFlush(user));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account could not be created");
        }
    }
    @Transactional
    public EmployeeResponse changeRole(long id, Role role) {
        var user = mutableEmployee(id);
        if (role == Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Administrator promotion is not supported");
        if (user.getRole() != role) {
            user.changeRole(role);
            sessions.revokeAllForUser(id, java.time.Instant.now());
        }
        return EmployeeResponse.from(user);
    }

    @Transactional
    public EmployeeResponse changeStatus(long id, boolean enabled) {
        var user = mutableEmployee(id);
        if (user.isEnabled() != enabled) {
            user.setEnabled(enabled);
            sessions.revokeAllForUser(id, java.time.Instant.now());
        }
        return EmployeeResponse.from(user);
    }

    private AppUser mutableEmployee(long id) {
        var user = users.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        if (user.getRole() == Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator account is protected");
        return user;
    }
}

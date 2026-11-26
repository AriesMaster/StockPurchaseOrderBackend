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

    public EmployeeService(AppUserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
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
}

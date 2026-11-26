package com.stockpurchaseorder.backend.auth.service;

import com.stockpurchaseorder.backend.auth.domain.Role;
import com.stockpurchaseorder.backend.auth.dto.CreateEmployeeRequest;
import com.stockpurchaseorder.backend.auth.persistence.AppUser;
import com.stockpurchaseorder.backend.auth.persistence.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class EmployeeServiceTest {
    private AppUserRepository users;
    private PasswordEncoder passwords;
    private EmployeeService service;
    private com.stockpurchaseorder.backend.auth.persistence.AuthSessionRepository sessions;
    private static final String PASSWORD = "test-password-only-123";
    private static final String HASH = "$argon2id$test";

    @BeforeEach
    void setup() {
        users = mock(AppUserRepository.class);
        passwords = mock(PasswordEncoder.class);
        sessions = mock(com.stockpurchaseorder.backend.auth.persistence.AuthSessionRepository.class);
        service = new EmployeeService(users, passwords, sessions);
    }

    @Test
    void provisionsOperationalEmployeeWithNormalizedEmailAndHashedPassword() {
        when(passwords.encode(PASSWORD)).thenReturn(HASH);
        when(users.saveAndFlush(any(AppUser.class))).thenAnswer(call -> {
            AppUser user = call.getArgument(0);
            assertEquals(HASH, user.getPasswordHash());
            return user;
        });
        var response = service.create(new CreateEmployeeRequest("Buyer@Example.com", Role.PURCHASING_OFFICER, PASSWORD));
        assertEquals("buyer@example.com", response.email());
        assertEquals(Role.PURCHASING_OFFICER, response.role());
        assertTrue(response.enabled());
        verify(passwords).encode(PASSWORD);
    }

    @Test
    void rejectsDuplicateEmail() {
        when(users.existsByEmail("buyer@example.com")).thenReturn(true);
        var error = assertThrows(ResponseStatusException.class, () ->
                service.create(new CreateEmployeeRequest("Buyer@Example.com", Role.PURCHASING_OFFICER, PASSWORD)));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verifyNoInteractions(passwords);
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void rejectsAdditionalAdministrator() {
        var error = assertThrows(ResponseStatusException.class, () ->
                service.create(new CreateEmployeeRequest("admin@example.com", Role.ADMIN, PASSWORD)));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(users, passwords);
    }

    @Test
    void handlesConcurrentDuplicateInsert() {
        when(passwords.encode(PASSWORD)).thenReturn(HASH);
        when(users.saveAndFlush(any(AppUser.class))).thenThrow(new DataIntegrityViolationException("duplicate"));
        var error = assertThrows(ResponseStatusException.class, () ->
                service.create(new CreateEmployeeRequest("buyer@example.com", Role.APPROVER, PASSWORD)));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void returnsNotFoundForUnknownEmployee() {
        when(users.findById(99L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> service.get(99L)).getStatusCode());
    }

    @Test
    void rejectsInvalidPagination() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.list(0, 101)).getStatusCode());
        assertThrows(ResponseStatusException.class, () -> service.list(-1, 20));
        assertThrows(ResponseStatusException.class, () -> service.list(0, 0));
        verifyNoInteractions(users);
    }
    @Test
    void roleChangeRevokesSessions() {
        var user = new AppUser("buyer@example.com", Role.PURCHASING_OFFICER);
        user.activate(HASH);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        assertEquals(Role.APPROVER, service.changeRole(1L, Role.APPROVER).role());
        verify(sessions).revokeAllForUser(eq(1L), any(java.time.Instant.class));
    }

    @Test
    void statusChangeRevokesSessions() {
        var user = new AppUser("buyer@example.com", Role.PURCHASING_OFFICER);
        user.activate(HASH);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        assertFalse(service.changeStatus(1L, false).enabled());
        assertTrue(service.changeStatus(1L, true).enabled());
        verify(sessions, times(2)).revokeAllForUser(eq(1L), any(java.time.Instant.class));
    }

    @Test
    void administratorCannotBeDisabledOrDemoted() {
        when(users.findById(1L)).thenReturn(Optional.of(new AppUser("admin@example.com", Role.ADMIN)));
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> service.changeStatus(1L, false)).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> service.changeRole(1L, Role.APPROVER)).getStatusCode());
        verifyNoInteractions(sessions);
    }
}

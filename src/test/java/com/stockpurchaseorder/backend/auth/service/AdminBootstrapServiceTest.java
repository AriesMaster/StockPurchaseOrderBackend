package com.stockpurchaseorder.backend.auth.service;

import com.stockpurchaseorder.backend.auth.domain.Role;
import com.stockpurchaseorder.backend.auth.persistence.AppUser;
import com.stockpurchaseorder.backend.auth.persistence.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminBootstrapServiceTest {
    private final AppUserRepository users = mock(AppUserRepository.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);
    private final AdminBootstrapService service = new AdminBootstrapService(users, encoder, jdbc);

    @Test
    void createsEnabledAdministratorWithHashedPassword() {
        when(users.saveAndFlush(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            assertEquals("admin@example.test", user.getEmail());
            assertEquals(Role.ADMIN, user.getRole());
            assertTrue(user.isEnabled());
            assertTrue(encoder.matches("test-only-password", user.getPasswordHash()));
            return userWithId();
        });
        assertEquals(1L, service.provision(" ADMIN@example.test ", "test-only-password"));
        verify(jdbc).execute("LOCK TABLE app_user IN SHARE ROW EXCLUSIVE MODE");
    }

    private AppUser userWithId() {
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(1L);
        return user;
    }

    @Test
    void rejectsNonEmptyUserTable() {
        when(users.count()).thenReturn(1L);
        assertThrows(IllegalStateException.class, () -> service.provision("admin@example.test", "test-only-password"));
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void rejectsShortPassword() {
        assertThrows(IllegalArgumentException.class, () -> service.provision("admin@example.test", "short"));
        verifyNoInteractions(users, jdbc);
    }

    @Test
    void rejectsInvalidEmail() {
        assertThrows(IllegalArgumentException.class, () -> service.provision("invalid", "test-only-password"));
        verifyNoInteractions(users, jdbc);
    }
}

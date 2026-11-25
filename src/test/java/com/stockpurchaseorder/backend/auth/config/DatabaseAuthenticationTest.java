package com.stockpurchaseorder.backend.auth.config;

import com.stockpurchaseorder.backend.auth.domain.Role;
import com.stockpurchaseorder.backend.auth.persistence.AppUser;
import com.stockpurchaseorder.backend.auth.persistence.AppUserRepository;
import com.stockpurchaseorder.backend.auth.service.DatabaseUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseAuthenticationTest {
    private final AppUserRepository repository = mock(AppUserRepository.class);
    private final SecurityConfig config = new SecurityConfig();
    private final org.springframework.security.crypto.password.PasswordEncoder encoder = config.passwordEncoder();
    private final org.springframework.security.authentication.AuthenticationManager manager =
            config.authenticationManager(new DatabaseUserDetailsService(repository), encoder);

    private void account(boolean enabled) {
        AppUser user = mock(AppUser.class);
        when(user.getEmail()).thenReturn("admin@example.com");
        when(user.getRole()).thenReturn(Role.ADMIN);
        when(user.isEnabled()).thenReturn(enabled);
        when(user.getPasswordHash()).thenReturn(encoder.encode("test-password"));
        when(repository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
    }

    @Test
    void authenticatesNormalizedEmailAndAssignsRole() {
        account(true);
        var result = manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(" ADMIN@example.com ", "test-password"));
        assertTrue(result.isAuthenticated());
        assertTrue(result.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void rejectsWrongPassword() {
        account(true);
        assertThrows(BadCredentialsException.class, () -> manager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("admin@example.com", "wrong")));
    }

    @Test
    void rejectsDisabledAccount() {
        account(false);
        assertThrows(DisabledException.class, () -> manager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("admin@example.com", "test-password")));
    }

    @Test
    void rejectsUnknownAccount() {
        when(repository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
        assertThrows(BadCredentialsException.class, () -> manager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("unknown@example.com", "test-password")));
    }
}

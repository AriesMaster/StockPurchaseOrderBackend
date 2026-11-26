package com.stockpurchaseorder.backend.auth.service;

import com.stockpurchaseorder.backend.auth.domain.Role;
import com.stockpurchaseorder.backend.auth.persistence.AppUser;
import com.stockpurchaseorder.backend.auth.persistence.AppUserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminBootstrapService {
    private final AppUserRepository users;
    private final PasswordEncoder passwords;
    private final JdbcTemplate jdbc;

    public AdminBootstrapService(AppUserRepository users, PasswordEncoder passwords, JdbcTemplate jdbc) {
        this.users = users;
        this.passwords = passwords;
        this.jdbc = jdbc;
    }

    @Transactional
    public long provision(String email, String password) {
        String normalized = AppUser.normalizeEmail(email);
        if (normalized.length() > 254 || !normalized.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new IllegalArgumentException("Valid email required");
        if (password == null || password.length() < 15 || password.length() > 128)
            throw new IllegalArgumentException("Password must contain 15 to 128 characters");
        // Serialize bootstrap attempts across application instances.
        jdbc.execute("LOCK TABLE app_user IN SHARE ROW EXCLUSIVE MODE");
        if (users.count() != 0) throw new IllegalStateException("Bootstrap requires an empty user table");
        var admin = new AppUser(normalized, Role.ADMIN);
        admin.activate(passwords.encode(password));
        return users.saveAndFlush(admin).getId();
    }
}

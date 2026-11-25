package com.stockpurchaseorder.backend.auth.service;

import com.stockpurchaseorder.backend.auth.persistence.AppUser;
import com.stockpurchaseorder.backend.auth.persistence.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final AppUserRepository repository;

    public DatabaseUserDetailsService(AppUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        if (email == null || email.isBlank()) {
            throw new UsernameNotFoundException("Invalid credentials");
        }
        AppUser user = repository.findByEmail(AppUser.normalizeEmail(email))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new UsernameNotFoundException("Invalid credentials");
        }
        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .disabled(!user.isEnabled())
                .build();
    }
}

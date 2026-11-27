package com.stockpurchaseorder.backend.auth.config;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfToken;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication(type = org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET)
    SecurityFilterChain securityFilterChain(HttpSecurity http, com.stockpurchaseorder.backend.auth.service.SessionJwtDecoder decoder) throws Exception {
        var csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookieCustomizer(cookie -> cookie.secure(true).sameSite("Strict").path("/"));
        var csrfHandler = new CsrfTokenRequestAttributeHandler() {
            @Override
            public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken token) {
                return request.getHeader(token.getHeaderName());
            }
        };
        var chain = http
                .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository).csrfTokenRequestHandler(csrfHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(oauth -> oauth.bearerTokenResolver(request -> {
                    if ("POST".equals(request.getMethod()) &&
                            java.util.Set.of("/api/auth/login", "/api/auth/refresh", "/api/auth/logout").contains(request.getServletPath().isEmpty() ? request.getRequestURI().substring(request.getContextPath().length()) : request.getServletPath())) return null;
                    return new org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver().resolve(request);
                }).jwt(jwt -> jwt.decoder(decoder)
                        .jwtAuthenticationConverter(token -> new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(
                                token, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + token.getClaimAsString("role")))))))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/employees", "/api/employees/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/employees").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/employees/*/role", "/api/employees/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/suppliers", "/api/suppliers/*").hasAnyRole("ADMIN", "PURCHASING_OFFICER", "APPROVER", "WAREHOUSE_OFFICER")
                        .requestMatchers(HttpMethod.POST, "/api/suppliers").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/suppliers/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/suppliers/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/*").hasAnyRole("ADMIN", "PURCHASING_OFFICER", "APPROVER", "WAREHOUSE_OFFICER")
                        .requestMatchers(HttpMethod.POST, "/api/products").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/products/*/status").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .build();
        // Resource-server configuration exempts bearer requests by default.
        // StockFlow deliberately requires CSRF for every unsafe HTTP method.
        chain.getFilters().stream()
                .filter(org.springframework.security.web.csrf.CsrfFilter.class::isInstance)
                .map(org.springframework.security.web.csrf.CsrfFilter.class::cast)
                .forEach(filter -> filter.setRequireCsrfProtectionMatcher(
                        org.springframework.security.web.csrf.CsrfFilter.DEFAULT_CSRF_MATCHER));
        return chain;
    }
}

package com.stockpurchaseorder.backend.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class PostgresMigrationTest {
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("stockflow_test")
            .withInitScript("postgres-test-init.sql");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired JdbcTemplate jdbc;

    @Test
    void migrationsCreateAuthenticationSchema() {
        assertEquals("stockflow_test", jdbc.queryForObject("select current_database()", String.class));
        assertEquals(2, jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success and version in ('1','2')", Integer.class));
        assertEquals(4, jdbc.queryForObject(
                "select count(*) from pg_tables where schemaname = 'public' and rowsecurity and tablename in ('app_user','auth_session','refresh_token','account_token')", Integer.class));
        assertEquals(Boolean.FALSE, jdbc.queryForObject(
                "select has_table_privilege('anon', 'public.app_user', 'SELECT')", Boolean.class));
    }

    @Autowired SessionService sessions;
    @Autowired RefreshTokenService tokens;
    @Autowired AccessTokenService access;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;

    private long user(boolean enabled) {
        return jdbc.queryForObject(
                "insert into app_user(email,password_hash,role,enabled) values (?,?, 'ADMIN',?) returning id",
                Long.class, java.util.UUID.randomUUID() + "@example.test", passwords.encode("test-only-password"), enabled);
    }

    private boolean revoked(java.util.UUID id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select revoked_at is not null from auth_session where id=?", Boolean.class, id));
    }

    @Test
    void createsSessionAndStoresOnlyRefreshDigest() {
        long id = user(true);
        var credentials = sessions.create(id).orElseThrow();
        assertEquals(Long.toString(id), access.decode(credentials.accessToken()).getSubject());
        assertEquals(id, jdbc.queryForObject("select user_id from auth_session where id=?", Long.class, credentials.sessionId()));
        assertEquals(tokens.digest(credentials.refreshToken()), jdbc.queryForObject(
                "select token_digest from refresh_token where session_id=?", String.class, credentials.sessionId()));
        assertFalse(revoked(credentials.sessionId()));
    }

    @Test
    void rotatesTokenAndConsumesPreviousToken() {
        var first = sessions.create(user(true)).orElseThrow();
        var second = sessions.rotate(first.refreshToken()).orElseThrow();
        assertEquals(first.sessionId(), second.sessionId());
        assertNotEquals(first.refreshToken(), second.refreshToken());
        assertEquals(Boolean.TRUE, jdbc.queryForObject(
                "select consumed_at is not null from refresh_token where token_digest=?",
                Boolean.class, tokens.digest(first.refreshToken())));
        assertEquals(2, jdbc.queryForObject("select count(*) from refresh_token where session_id=?", Integer.class, first.sessionId()));
    }

    @Test
    void reusedTokenRevokesEntireSession() {
        var first = sessions.create(user(true)).orElseThrow();
        var second = sessions.rotate(first.refreshToken()).orElseThrow();
        assertTrue(sessions.rotate(first.refreshToken()).isEmpty());
        assertTrue(revoked(first.sessionId()));
        assertTrue(sessions.rotate(second.refreshToken()).isEmpty());
    }

    @Test
    void onlyOwnerCanRevokeSession() {
        long owner = user(true);
        var first = sessions.create(owner).orElseThrow();
        sessions.revoke(first.sessionId(), user(true));
        assertFalse(revoked(first.sessionId()));
        sessions.revoke(first.sessionId(), owner);
        assertTrue(revoked(first.sessionId()));
        assertTrue(sessions.rotate(first.refreshToken()).isEmpty());
    }

    @Test
    void disabledUsersCannotCreateOrRefreshSessions() {
        assertTrue(sessions.create(user(false)).isEmpty());
        long id = user(true);
        var first = sessions.create(id).orElseThrow();
        jdbc.update("update app_user set enabled=false where id=?", id);
        assertTrue(sessions.rotate(first.refreshToken()).isEmpty());
        assertTrue(revoked(first.sessionId()));
    }

    @Test
    void concurrentRefreshAllowsOnlyOneRotationAndRevokesOnReuse() throws Exception {
        var first = sessions.create(user(true)).orElseThrow();
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try {
            java.util.concurrent.Callable<java.util.Optional<SessionService.Credentials>> task = () -> {
                if (!start.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Start timeout");
                return sessions.rotate(first.refreshToken());
            };
            var a = pool.submit(task);
            var b = pool.submit(task);
            start.countDown();
            var resultA = a.get(30, java.util.concurrent.TimeUnit.SECONDS);
            var resultB = b.get(30, java.util.concurrent.TimeUnit.SECONDS);
            assertNotEquals(resultA.isPresent(), resultB.isPresent());
            assertTrue(revoked(first.sessionId()));
            assertEquals(2, jdbc.queryForObject("select count(*) from refresh_token where session_id=?", Integer.class, first.sessionId()));
        } finally {
            pool.shutdownNow();
        }
    }

    @Autowired AuthenticationService authentication;

    private String email(long id) {
        return jdbc.queryForObject("select email from app_user where id=?", String.class, id);
    }

    private com.stockpurchaseorder.backend.auth.dto.LoginRequest loginRequest(String email, String password) {
        return new com.stockpurchaseorder.backend.auth.dto.LoginRequest(email, password);
    }

    @Test
    void authenticationLoginReturnsSafeUserAndValidCredentials() {
        long id = user(true);
        String email = email(id);
        var result = authentication.login(loginRequest(" " + email.toUpperCase(java.util.Locale.ROOT) + " ", "test-only-password"));
        assertEquals(id, result.response().user().id());
        assertEquals(email, result.response().user().email());
        assertEquals(com.stockpurchaseorder.backend.auth.domain.Role.ADMIN, result.response().user().role());
        var jwt = access.decode(result.response().accessToken());
        assertEquals(Long.toString(id), jwt.getSubject());
        assertEquals(1, jdbc.queryForObject("select count(*) from refresh_token where token_digest=?", Integer.class, tokens.digest(result.refreshToken())));
    }

    @Test
    void invalidLoginDoesNotCreateSession() {
        long id = user(true);
        var error = assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> authentication.login(loginRequest(email(id), "wrong-password")));
        assertEquals("Invalid credentials", error.getMessage());
        assertEquals(0, jdbc.queryForObject("select count(*) from auth_session where user_id=?", Integer.class, id));
    }

    @Test
    void unknownAndDisabledAccountsReturnSameError() {
        long id = user(false);
        var disabled = assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> authentication.login(loginRequest(email(id), "test-only-password")));
        var unknown = assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> authentication.login(loginRequest(java.util.UUID.randomUUID() + "@example.test", "test-only-password")));
        assertEquals("Invalid credentials", disabled.getMessage());
        assertEquals(disabled.getMessage(), unknown.getMessage());
        assertEquals(0, jdbc.queryForObject("select count(*) from auth_session where user_id=?", Integer.class, id));
    }

    @Test
    void authenticationRefreshReturnsNewCredentialsAndRejectsReplay() {
        long id = user(true);
        var first = authentication.login(loginRequest(email(id), "test-only-password"));
        var second = authentication.refresh(first.refreshToken());
        assertEquals(first.response().user(), second.response().user());
        assertNotEquals(first.refreshToken(), second.refreshToken());
        assertNotEquals(first.response().accessToken(), second.response().accessToken());
        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> authentication.refresh(first.refreshToken()));
        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> authentication.refresh(second.refreshToken()));
    }

    @Test
    void malformedAndUnknownRefreshTokensAreRejected() {
        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> authentication.refresh("invalid"));
        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> authentication.refresh(tokens.generate()));
    }

    @Autowired org.springframework.web.context.WebApplicationContext web;
    @Autowired org.springframework.security.web.FilterChainProxy security;

    private org.springframework.test.web.servlet.MockMvc mvc() {
        return org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(web)
                .addFilters(security).build();
    }

    private jakarta.servlet.http.Cookie csrfCookie(org.springframework.test.web.servlet.MockMvc mvc) throws Exception {
        var response = mvc.perform(get("/api/auth/csrf").secure(true))
                .andExpect(status().isNoContent()).andReturn().getResponse();
        var cookie = response.getCookie("XSRF-TOKEN");
        assertNotNull(cookie);
        assertTrue(cookie.getSecure());
        assertFalse(cookie.isHttpOnly());
        return cookie;
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder loginHttp(String email, String password) {
        return post("/api/auth/login").secure(true).contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
    }

    @Test
    void httpLoginIssuesSecureCookieAndSafeJson() throws Exception {
        var mvc = mvc();
        var csrf = csrfCookie(mvc);
        long id = user(true);
        var response = mvc.perform(loginHttp(email(id), "test-only-password")
                .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(id))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse();
        var refresh = response.getCookie("__Secure-stockflow-refresh");
        assertNotNull(refresh);
        assertTrue(refresh.isHttpOnly());
        assertTrue(refresh.getSecure());
        assertEquals("/api/auth", refresh.getPath());
        assertTrue(response.getHeaders("Set-Cookie").stream().anyMatch(h -> h.contains("SameSite=Strict") && h.contains("__Secure-stockflow-refresh")));
        assertFalse(response.getContentAsString().contains(refresh.getValue()));
    }

    @Test
    void httpLoginRejectsMissingCsrf() throws Exception {
        mvc().perform(loginHttp(email(user(true)), "test-only-password"))
                .andExpect(status().isForbidden());
    }

    @Test
    void httpLoginRejectsIncorrectCsrf() throws Exception {
        var mvc = mvc();
        var csrf = csrfCookie(mvc);
        mvc.perform(loginHttp(email(user(true)), "test-only-password")
                .cookie(csrf).header("X-XSRF-TOKEN", "incorrect"))
                .andExpect(status().isForbidden());
    }

    @Test
    void httpLoginRejectsWrongPassword() throws Exception {
        var mvc = mvc();
        var csrf = csrfCookie(mvc);
        mvc.perform(loginHttp(email(user(true)), "wrong")
                .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void httpRefreshRotatesAndRejectsReplay() throws Exception {
        var mvc = mvc();
        var csrf = csrfCookie(mvc);
        var first = mvc.perform(loginHttp(email(user(true)), "test-only-password")
                .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("__Secure-stockflow-refresh");
        assertNotNull(first);
        var second = mvc.perform(post("/api/auth/refresh").secure(true).cookie(csrf, first)
                .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("__Secure-stockflow-refresh");
        assertNotNull(second);
        assertNotEquals(first.getValue(), second.getValue());
        mvc.perform(post("/api/auth/refresh").secure(true).cookie(csrf, first)
                .header("X-XSRF-TOKEN", csrf.getValue())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").secure(true).cookie(csrf, second)
                .header("X-XSRF-TOKEN", csrf.getValue())).andExpect(status().isUnauthorized());
    }

    @Test
    void httpRefreshRequiresCsrfAndOtherEndpointsRemainDenied() throws Exception {
        var mvc = mvc();
        mvc.perform(post("/api/auth/refresh").secure(true)).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me").secure(true)).andExpect(status().isUnauthorized());
    }

    @Test
    void httpMeAcceptsValidBearerToken() throws Exception {
        long id = user(true);
        var result = authentication.login(loginRequest(email(id), "test-only-password"));
        mvc().perform(get("/api/auth/me").secure(true)
                .header("Authorization", "Bearer " + result.response().accessToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email(id)))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void httpMeRejectsRevokedSession() throws Exception {
        long id = user(true);
        var credentials = sessions.create(id).orElseThrow();
        sessions.revoke(credentials.sessionId(), id);
        mvc().perform(get("/api/auth/me").secure(true)
                .header("Authorization", "Bearer " + credentials.accessToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void httpMeRejectsDisabledAccount() throws Exception {
        long id = user(true);
        var credentials = sessions.create(id).orElseThrow();
        jdbc.update("update app_user set enabled=false where id=?", id);
        mvc().perform(get("/api/auth/me").secure(true)
                .header("Authorization", "Bearer " + credentials.accessToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void httpMeRejectsStaleRole() throws Exception {
        long id = user(true);
        var credentials = sessions.create(id).orElseThrow();
        jdbc.update("update app_user set role='APPROVER' where id=?", id);
        mvc().perform(get("/api/auth/me").secure(true)
                .header("Authorization", "Bearer " + credentials.accessToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void httpMeRejectsInvalidJwt() throws Exception {
        mvc().perform(get("/api/auth/me").secure(true)
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }
}

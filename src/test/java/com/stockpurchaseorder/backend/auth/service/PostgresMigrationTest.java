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
}

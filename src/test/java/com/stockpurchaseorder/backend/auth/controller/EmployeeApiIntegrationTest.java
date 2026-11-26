package com.stockpurchaseorder.backend.auth.controller;

import com.stockpurchaseorder.backend.auth.domain.Role;
import com.stockpurchaseorder.backend.auth.persistence.AppUser;
import com.stockpurchaseorder.backend.auth.persistence.AppUserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:employee-api;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class EmployeeApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder passwords;
    private final ObjectMapper json = new ObjectMapper();
    private static final String PASSWORD = "integration-test-password";
    private static final String ADMIN = "employee-test-admin@example.com";
    private static final String BUYER = "employee-test-buyer@example.com";

    @BeforeEach
    void seed() {
        seedUser(ADMIN, Role.ADMIN);
        seedUser(BUYER, Role.PURCHASING_OFFICER);
    }

    private void seedUser(String email, Role role) {
        if (!users.existsByEmail(email)) {
            var user = new AppUser(email, role);
            user.activate(passwords.encode(PASSWORD));
            users.saveAndFlush(user);
        }
    }

    private Cookie csrf() throws Exception {
        return mvc.perform(get("/api/auth/csrf")).andExpect(status().isNoContent())
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
    }

    private String login(String email) throws Exception {
        return json.readTree(loginResult(email).getResponse().getContentAsString()).get("accessToken").asText();
    }

    private MvcResult loginResult(String email) throws Exception {
        Cookie csrf = csrf();
        return mvc.perform(post("/api/auth/login").cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue()).contentType("application/json")
                .content(json.writeValueAsString(java.util.Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder create(String token, String email, String password) throws Exception {
        Cookie csrf = csrf();
        return post("/api/employees").cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                .header("Authorization", "Bearer " + token).contentType("application/json")
                .content(json.writeValueAsString(java.util.Map.of("email", email, "password", password, "role", "APPROVER")));
    }

    @Test
    void anonymousCannotListEmployees() throws Exception {
        mvc.perform(get("/api/employees")).andExpect(status().isUnauthorized());
    }

    @Test
    void operationalUserCannotListOrCreateEmployees() throws Exception {
        String token = login(BUYER);
        mvc.perform(get("/api/employees").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(create(token, "forbidden@example.com", PASSWORD)).andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateAndRetrieveEmployeeWithoutExposingPassword() throws Exception {
        String token = login(ADMIN);
        String email = "new-" + java.util.UUID.randomUUID() + "@example.com";
        mvc.perform(create(token, email, PASSWORD)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
        var user = users.findByEmail(email).orElseThrow();
        assertTrue(passwords.matches(PASSWORD, user.getPasswordHash()));
        mvc.perform(get("/api/employees/" + user.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(get("/api/employees").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void invalidPasswordIsRejected() throws Exception {
        mvc.perform(create(login(ADMIN), "invalid@example.com", "short"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        mvc.perform(create(login(ADMIN), BUYER, PASSWORD)).andExpect(status().isConflict());
    }

    @Test
    void missingCsrfIsRejected() throws Exception {
        mvc.perform(post("/api/employees").header("Authorization", "Bearer " + login(ADMIN))
                .contentType("application/json").content("{}")).andExpect(status().isForbidden());
    }
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder change(
            String token, long id, String field, String body) throws Exception {
        Cookie csrf = csrf();
        return patch("/api/employees/" + id + "/" + field).cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue()).header("Authorization", "Bearer " + token)
                .contentType("application/json").content(body);
    }

    private String freshEmployee() {
        String email = "lifecycle-" + java.util.UUID.randomUUID() + "@example.com";
        seedUser(email, Role.PURCHASING_OFFICER);
        return email;
    }

    private void assertRevoked(MvcResult credentials) throws Exception {
        String token = json.readTree(credentials.getResponse().getContentAsString()).get("accessToken").asText();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        Cookie csrf = csrf();
        Cookie refresh = credentials.getResponse().getCookie("__Secure-stockflow-refresh");
        assertNotNull(refresh);
        mvc.perform(post("/api/auth/refresh").cookie(csrf, refresh)
                .header("X-XSRF-TOKEN", csrf.getValue())).andExpect(status().isUnauthorized());
    }

    @Test
    void deactivationRevokesCredentialsAndReactivationRequiresNewLogin() throws Exception {
        String email = freshEmployee();
        long id = users.findByEmail(email).orElseThrow().getId();
        MvcResult credentials = loginResult(email);
        String admin = login(ADMIN);
        mvc.perform(change(admin, id, "status", "{\"enabled\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false));
        assertRevoked(credentials);
        Cookie csrf = csrf();
        mvc.perform(post("/api/auth/login").cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                .contentType("application/json").content(json.writeValueAsString(java.util.Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isUnauthorized());
        mvc.perform(change(admin, id, "status", "{\"enabled\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true));
        assertRevoked(credentials);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + login(email)))
                .andExpect(status().isOk());
    }

    @Test
    void roleChangeRevokesCredentialsAndNewLoginReflectsNewRole() throws Exception {
        String email = freshEmployee();
        long id = users.findByEmail(email).orElseThrow().getId();
        MvcResult credentials = loginResult(email);
        mvc.perform(change(login(ADMIN), id, "role", "{\"role\":\"APPROVER\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("APPROVER"));
        assertRevoked(credentials);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + login(email)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("APPROVER"));
    }

    @Test
    void administratorAccountIsProtected() throws Exception {
        long id = users.findByEmail(ADMIN).orElseThrow().getId();
        String token = login(ADMIN);
        mvc.perform(change(token, id, "status", "{\"enabled\":false}")).andExpect(status().isForbidden());
        mvc.perform(change(token, id, "role", "{\"role\":\"APPROVER\"}")).andExpect(status().isForbidden());
    }

    @Test
    void operationalEmployeeCannotChangeAccounts() throws Exception {
        long id = users.findByEmail(BUYER).orElseThrow().getId();
        String token = login(BUYER);
        mvc.perform(change(token, id, "role", "{\"role\":\"APPROVER\"}")).andExpect(status().isForbidden());
        mvc.perform(change(token, id, "status", "{\"enabled\":false}")).andExpect(status().isForbidden());
    }
    @Test
    void paginationHasStableContract() throws Exception {
        var result = mvc.perform(get("/api/employees").param("page", "0").param("size", "1")
                .header("Authorization", "Bearer " + login(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(users.count()))
                .andExpect(jsonPath("$.totalPages").value(users.count()))
                .andExpect(jsonPath("$.pageable").doesNotExist())
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andReturn();
        assertEquals(5, json.readTree(result.getResponse().getContentAsString()).size());
    }

    @Test
    void validationResponseDoesNotExposeRejectedPassword() throws Exception {
        String rejected = "secret-short";
        var result = mvc.perform(create(login(ADMIN), "invalid@example.com", rejected))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.fields[0]").value("password"))
                .andReturn();
        assertFalse(result.getResponse().getContentAsString().contains(rejected));
    }
}

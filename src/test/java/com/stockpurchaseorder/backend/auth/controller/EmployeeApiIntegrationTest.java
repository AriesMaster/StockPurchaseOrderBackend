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
        Cookie csrf = csrf();
        MvcResult result = mvc.perform(post("/api/auth/login").cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue()).contentType("application/json")
                .content(json.writeValueAsString(java.util.Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
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
}

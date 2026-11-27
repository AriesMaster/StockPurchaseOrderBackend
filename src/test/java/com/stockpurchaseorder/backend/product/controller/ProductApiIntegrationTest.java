package com.stockpurchaseorder.backend.product.controller;

import com.stockpurchaseorder.backend.auth.domain.Role;
import com.stockpurchaseorder.backend.auth.persistence.*;
import com.stockpurchaseorder.backend.product.repository.ProductRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:product-api;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop","spring.flyway.enabled=false"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ProductApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired ProductRepository products;
    @Autowired PasswordEncoder passwords;
    private final ObjectMapper json = new ObjectMapper();
    private static final String BODY = "{\"sku\":\"PAINT-20L\",\"name\":\"White paint\",\"unitPrice\":125.50}";
    private static final String UPDATE = "{\"name\":\"Premium paint\",\"unitPrice\":150.00}";

    @BeforeEach void seed() {
        products.deleteAll();
        for (Role role : Role.values()) {
            String email = email(role);
            if (!users.existsByEmail(email)) {
                var user = new AppUser(email, role);
                user.activate(passwords.encode("integration-test-password"));
                users.saveAndFlush(user);
            }
        }
    }
    private String email(Role role) {
        return role.name().toLowerCase(java.util.Locale.ROOT)+"@product-test.example";
    }
    private Cookie csrf() throws Exception {
        return mvc.perform(get("/api/auth/csrf")).andExpect(status().isNoContent()).andReturn().getResponse().getCookie("XSRF-TOKEN");
    }
    private String login(Role role) throws Exception {
        var c = csrf();
        var response = mvc.perform(post("/api/auth/login").cookie(c).header("X-XSRF-TOKEN", c.getValue())
            .contentType("application/json").content(json.writeValueAsString(java.util.Map.of("email",email(role),"password","integration-test-password"))))
            .andExpect(status().isOk()).andReturn().getResponse();
        return json.readTree(response.getContentAsString()).get("accessToken").asText();
    }
    private MockHttpServletRequestBuilder write(MockHttpServletRequestBuilder request, String token, String body) throws Exception {
        var c = csrf();
        return request.cookie(c).header("X-XSRF-TOKEN", c.getValue()).header("Authorization","Bearer "+token).contentType("application/json").content(body);
    }
    private void create(String token) throws Exception {
        mvc.perform(write(post("/api/products"),token,BODY)).andExpect(status().isCreated())
            .andExpect(jsonPath("$.sku").value("PAINT-20L")).andExpect(jsonPath("$.active").value(true));
    }

    @Test void anonymousCannotRead() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
    }
    @Test void adminCanCreateUpdateDeactivateAndReactivate() throws Exception {
        String token = login(Role.ADMIN);
        create(token);
        mvc.perform(write(put("/api/products/PAINT-20L"),token,UPDATE)).andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Premium paint")).andExpect(jsonPath("$.unitPrice").value(150.0))
            .andExpect(jsonPath("$.sku").value("PAINT-20L"));
        mvc.perform(write(patch("/api/products/PAINT-20L/status"),token,"{\"active\":false}")).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        assertFalse(products.findById("PAINT-20L").orElseThrow().isActive());
        mvc.perform(write(patch("/api/products/PAINT-20L/status"),token,"{\"active\":true}")).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true));
    }
    @Test void rejectsDuplicateWithoutOverwritingOriginal() throws Exception {
        String token = login(Role.ADMIN);
        create(token);
        mvc.perform(write(post("/api/products"),token,BODY.replace("White paint","Changed"))).andExpect(status().isConflict());
        assertEquals("White paint",products.findById("PAINT-20L").orElseThrow().getName());
        assertEquals(1,products.count());
    }
    @Test void operationalRolesCanReadButNotWrite() throws Exception {
        create(login(Role.ADMIN));
        for (Role role : new Role[]{Role.PURCHASING_OFFICER,Role.APPROVER,Role.WAREHOUSE_OFFICER}) {
            String token = login(role);
            mvc.perform(get("/api/products").header("Authorization","Bearer "+token)).andExpect(status().isOk());
            mvc.perform(get("/api/products/PAINT-20L").header("Authorization","Bearer "+token)).andExpect(status().isOk());
            mvc.perform(write(post("/api/products"),token,BODY)).andExpect(status().isForbidden());
            mvc.perform(write(put("/api/products/PAINT-20L"),token,UPDATE)).andExpect(status().isForbidden());
            mvc.perform(write(patch("/api/products/PAINT-20L/status"),token,"{\"active\":false}")).andExpect(status().isForbidden());
        }
        assertTrue(products.findById("PAINT-20L").orElseThrow().isActive());
    }
    @Test void rejectsMissingCsrf() throws Exception {
        mvc.perform(post("/api/products").header("Authorization","Bearer "+login(Role.ADMIN)).contentType("application/json").content(BODY)).andExpect(status().isForbidden());
        assertEquals(0,products.count());
    }
    @Test void validatesSkuNameAndPrice() throws Exception {
        String token = login(Role.ADMIN);
        for (String body : new String[]{BODY.replace("PAINT-20L","bad/sku"),BODY.replace("White paint"," "),BODY.replace("125.50","-1"),BODY.replace("125.50","1.001")}) {
            mvc.perform(write(post("/api/products"),token,body)).andExpect(status().isBadRequest());
        }
        assertEquals(0,products.count());
    }
    @Test void searchByNameAndSkuAndPaginate() throws Exception {
        String token = login(Role.ADMIN);
        create(token);
        mvc.perform(write(post("/api/products"),token,"{\"sku\":\"NAIL-1\",\"name\":\"Nails\",\"unitPrice\":20}")).andExpect(status().isCreated());
        for (String search : new String[]{"white","paint-20l"}) {
            mvc.perform(get("/api/products").param("search",search).param("size","1").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].sku").value("PAINT-20L")).andExpect(jsonPath("$.totalElements").value(1));
        }
        mvc.perform(get("/api/products").param("page","1").param("size","1").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.number").value(1)).andExpect(jsonPath("$.totalElements").value(2));
    }
    @Test void missingProductReturnsNotFound() throws Exception {
        mvc.perform(get("/api/products/MISSING").header("Authorization","Bearer "+login(Role.ADMIN))).andExpect(status().isNotFound());
    }
}

package com.stockpurchaseorder.backend.supplier.controller;
import com.stockpurchaseorder.backend.auth.domain.Role;
import com.stockpurchaseorder.backend.auth.persistence.*;
import com.stockpurchaseorder.backend.supplier.persistence.SupplierRepository;
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

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:supplier-api;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop","spring.flyway.enabled=false"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SupplierApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired SupplierRepository suppliers;
    @Autowired PasswordEncoder passwords;
    private final ObjectMapper json = new ObjectMapper();
    private static final String BODY = "{\"name\":\"Hardware Supplies\",\"email\":\"sales@example.com\"}";
    @BeforeEach void seed() {
        suppliers.deleteAll();
        for (Role role : Role.values()) {
            String email = email(role);
            if (!users.existsByEmail(email)) {
                var u = new AppUser(email,role);
                u.activate(passwords.encode("integration-test-password"));
                users.saveAndFlush(u);
            }
        }
    }
    private String email(Role role) { return role.name().toLowerCase(java.util.Locale.ROOT)+"@supplier-test.example"; }
    private Cookie csrf() throws Exception {
        return mvc.perform(get("/api/auth/csrf")).andExpect(status().isNoContent()).andReturn().getResponse().getCookie("XSRF-TOKEN");
    }
    private String login(Role role) throws Exception {
        var c=csrf();
        var response=mvc.perform(post("/api/auth/login").cookie(c).header("X-XSRF-TOKEN",c.getValue())
            .contentType("application/json").content(json.writeValueAsString(java.util.Map.of("email",email(role),"password","integration-test-password"))))
            .andExpect(status().isOk()).andReturn().getResponse();
        return json.readTree(response.getContentAsString()).get("accessToken").asText();
    }
    private MockHttpServletRequestBuilder write(MockHttpServletRequestBuilder request,String token,String body) throws Exception {
        var c=csrf();
        return request.cookie(c).header("X-XSRF-TOKEN",c.getValue()).header("Authorization","Bearer "+token).contentType("application/json").content(body);
    }
    private long create(String token) throws Exception {
        var response=mvc.perform(write(post("/api/suppliers"),token,BODY)).andExpect(status().isCreated())
            .andExpect(jsonPath("$.active").value(true)).andExpect(jsonPath("$.createdAt").exists()).andReturn().getResponse();
        return json.readTree(response.getContentAsString()).get("id").asLong();
    }
    @Test void anonymousCannotRead() throws Exception {
        mvc.perform(get("/api/suppliers")).andExpect(status().isUnauthorized());
    }
    @Test void adminCanCreateUpdateDeactivateAndReactivate() throws Exception {
        String token=login(Role.ADMIN);
        long id=create(token);
        mvc.perform(write(put("/api/suppliers/"+id),token,"{\"name\":\"Updated\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated"));
        mvc.perform(write(patch("/api/suppliers/"+id+"/status"),token,"{\"active\":false}")).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        assertFalse(suppliers.findById(id).orElseThrow().isActive());
        mvc.perform(write(patch("/api/suppliers/"+id+"/status"),token,"{\"active\":true}")).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true));
    }
    @Test void operationalRolesCanReadButNotModify() throws Exception {
        long id=create(login(Role.ADMIN));
        for(Role role : new Role[]{Role.PURCHASING_OFFICER,Role.APPROVER,Role.WAREHOUSE_OFFICER}) {
            String token=login(role);
            mvc.perform(get("/api/suppliers/"+id).header("Authorization","Bearer "+token)).andExpect(status().isOk());
            mvc.perform(get("/api/suppliers").header("Authorization","Bearer "+token)).andExpect(status().isOk());
            mvc.perform(write(post("/api/suppliers"),token,BODY)).andExpect(status().isForbidden());
            mvc.perform(write(put("/api/suppliers/"+id),token,BODY)).andExpect(status().isForbidden());
            mvc.perform(write(patch("/api/suppliers/"+id+"/status"),token,"{\"active\":false}")).andExpect(status().isForbidden());
        }
        assertEquals(1,suppliers.count());
        assertTrue(suppliers.findById(id).orElseThrow().isActive());
    }
    @Test void rejectsMissingCsrf() throws Exception {
        mvc.perform(post("/api/suppliers").header("Authorization","Bearer "+login(Role.ADMIN)).contentType("application/json").content(BODY)).andExpect(status().isForbidden());
        assertEquals(0,suppliers.count());
    }
    @Test void validatesRequests() throws Exception {
        String token=login(Role.ADMIN);
        mvc.perform(write(post("/api/suppliers"),token,"{\"name\":\" \"}")).andExpect(status().isBadRequest());
        mvc.perform(write(post("/api/suppliers"),token,"{\"name\":\"Valid\",\"email\":\"invalid\"}")).andExpect(status().isBadRequest());
        assertEquals(0,suppliers.count());
    }
    @Test void searchAndPaginationUseStableContract() throws Exception {
        String token=login(Role.ADMIN);
        create(token);
        mvc.perform(write(post("/api/suppliers"),token,"{\"name\":\"Other\"}")).andExpect(status().isCreated());
        mvc.perform(get("/api/suppliers").param("search","HARDWARE").param("size","1").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1)).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.pageable").doesNotExist());
    }
    @Test void missingSupplierReturnsNotFound() throws Exception {
        mvc.perform(get("/api/suppliers/999999").header("Authorization","Bearer "+login(Role.ADMIN))).andExpect(status().isNotFound());
    }
    @Test void filtersStatusBeforePaginationAndCombinesWithSearch() throws Exception {
        String token=login(Role.ADMIN);
        long id=create(token);
        mvc.perform(write(post("/api/suppliers"),token,"{\"name\":\"Hardware Outlet\"}")).andExpect(status().isCreated());
        mvc.perform(write(patch("/api/suppliers/"+id+"/status"),token,"{\"active\":false}")).andExpect(status().isOk());
        mvc.perform(get("/api/suppliers").param("active","false").param("search","hardware").param("size","1").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].active").value(false));
        mvc.perform(get("/api/suppliers").param("active","true").param("search","hardware").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].name").value("Hardware Outlet"));
    }
}

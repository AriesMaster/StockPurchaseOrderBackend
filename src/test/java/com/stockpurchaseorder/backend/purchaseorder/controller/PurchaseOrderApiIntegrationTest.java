package com.stockpurchaseorder.backend.purchaseorder.controller;

import com.stockpurchaseorder.backend.auth.domain.Role;
import com.stockpurchaseorder.backend.auth.persistence.*;
import com.stockpurchaseorder.backend.product.persistence.ProductEntity;
import com.stockpurchaseorder.backend.product.repository.ProductRepository;
import com.stockpurchaseorder.backend.supplier.persistence.*;
import com.stockpurchaseorder.backend.purchaseorder.persistence.PurchaseOrderRepository;
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
import java.math.BigDecimal;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:order-api;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop","spring.flyway.enabled=false"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PurchaseOrderApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired ProductRepository products;
    @Autowired SupplierRepository suppliers;
    @Autowired PurchaseOrderRepository orders;
    @Autowired com.stockpurchaseorder.backend.supplier.catalogue.SupplierCatalogueService catalogue;
    @Autowired com.stockpurchaseorder.backend.supplier.catalogue.SupplierProductRepository offers;
    private final ObjectMapper json=new ObjectMapper();
    private long supplierId;

    @BeforeEach void seed(){
        orders.deleteAll();
        offers.deleteAll();
        products.deleteAll();
        suppliers.deleteAll();
        supplierId=suppliers.saveAndFlush(new SupplierEntity("Hardware",null,null,null,null)).getId();
        products.saveAndFlush(new ProductEntity("PAINT","White paint",new BigDecimal("115.00")));
        products.saveAndFlush(new ProductEntity("NAILS","Nails",new BigDecimal("25.50")));
        catalogue.put(supplierId,"PAINT",new BigDecimal("115.00"),true);
        catalogue.put(supplierId,"NAILS",new BigDecimal("25.50"),true);
        for(Role role:Role.values()){
            if(!users.existsByEmail(email(role))){
                var user=new AppUser(email(role),role);
                user.activate(passwords.encode("integration-test-password"));
                users.saveAndFlush(user);
            }
        }
    }
    private String email(Role role){return role.name().toLowerCase(java.util.Locale.ROOT)+"@order-test.example";}
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
    private MockHttpServletRequestBuilder createRequest(String token,String body) throws Exception{
        var c=csrf();
        return post("/api/purchase-orders").cookie(c).header("X-XSRF-TOKEN",c.getValue()).header("Authorization","Bearer "+token).contentType("application/json").content(body);
    }
    private String body(String lines){return "{\"supplierId\":"+supplierId+",\"lines\":"+lines+"}";}
    private String valid(){return body("[{\"sku\":\"PAINT\",\"orderedQuantity\":2},{\"sku\":\"NAILS\",\"orderedQuantity\":3}]");}
    private long create(String token) throws Exception{
        var result=mvc.perform(createRequest(token,valid())).andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT")).andExpect(jsonPath("$.supplierId").value(supplierId))
            .andExpect(jsonPath("$.total").value(306.5)).andExpect(jsonPath("$.lines.length()").value(2))
            .andExpect(jsonPath("$.lines[0].receivedQuantity").value(0))
            .andExpect(jsonPath("$.lines[0].outstandingQuantity").value(3))
            .andReturn().getResponse();
        return json.readTree(result.getContentAsString()).get("id").asLong();
    }
    @Test void purchasingOfficerCreatesAndAllRolesRead() throws Exception{
        long id=create(login(Role.PURCHASING_OFFICER));
        for(Role role:Role.values()){
            String token=login(role);
            mvc.perform(get("/api/purchase-orders/"+id).header("Authorization","Bearer "+token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(306.5));
            mvc.perform(get("/api/purchase-orders").param("status","DRAFT").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        }
    }
    @Test void adminCanCreateAndHistoricalPriceAndNameRemainUnchanged() throws Exception{
        String token=login(Role.ADMIN);
        long id=create(token);
        var paint=products.findById("PAINT").orElseThrow();
        paint.updateDetails("New paint",new BigDecimal("130.00"));
        products.saveAndFlush(paint);
        mvc.perform(get("/api/purchase-orders/"+id).header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.lines[1].productName").value("White paint"))
            .andExpect(jsonPath("$.lines[1].unitPrice").value(115.0)).andExpect(jsonPath("$.total").value(306.5));
    }
    @Test void rejectsUnauthorizedWritesAndAnonymousReads() throws Exception{
        mvc.perform(get("/api/purchase-orders")).andExpect(status().isUnauthorized());
        for(Role role:new Role[]{Role.APPROVER,Role.WAREHOUSE_OFFICER}){
            mvc.perform(createRequest(login(role),valid())).andExpect(status().isForbidden());
        }
        assertEquals(0,orders.count());
    }
    @Test void requiresCsrf() throws Exception{
        mvc.perform(post("/api/purchase-orders").header("Authorization","Bearer "+login(Role.ADMIN))
            .contentType("application/json").content(valid())).andExpect(status().isForbidden());
        assertEquals(0,orders.count());
    }
    @Test void rejectsInvalidLinesAndDuplicateSkus() throws Exception{
        String token=login(Role.ADMIN);
        for(String lines:new String[]{"[]","[null]","[{\"sku\":\"PAINT\",\"orderedQuantity\":0}]","[{\"sku\":\"PAINT\",\"orderedQuantity\":1},{\"sku\":\"PAINT\",\"orderedQuantity\":2}]"}){
            mvc.perform(createRequest(token,body(lines))).andExpect(status().isBadRequest());
        }
        assertEquals(0,orders.count());
    }
    @Test void rejectsUnknownProductAndInactiveProductWithoutPartialOrder() throws Exception{
        String token=login(Role.ADMIN);
        mvc.perform(createRequest(token,valid().replace("NAILS","MISSING"))).andExpect(status().is(422));
        var nails=products.findById("NAILS").orElseThrow();
        nails.changeStatus(false);products.saveAndFlush(nails);
        mvc.perform(createRequest(token,valid())).andExpect(status().isConflict());
        assertEquals(0,orders.count());
    }
    @Test void rejectsUnknownAndInactiveSupplier() throws Exception{
        String token=login(Role.ADMIN);
        mvc.perform(createRequest(token,valid().replace("\"supplierId\":"+supplierId,"\"supplierId\":99999999"))).andExpect(status().is(422));
        var supplier=suppliers.findById(supplierId).orElseThrow();
        supplier.changeStatus(false);suppliers.saveAndFlush(supplier);
        mvc.perform(createRequest(token,valid())).andExpect(status().isConflict());
        assertEquals(0,orders.count());
    }
    @Test void paginatesAndReturnsNotFound() throws Exception{
        String token=login(Role.ADMIN);
        long first=create(token);
        long second=create(token);
        mvc.perform(get("/api/purchase-orders").param("size","1").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.content[0].id").value(second));
        mvc.perform(get("/api/purchase-orders").param("size","1").param("page","1").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(first));
        mvc.perform(get("/api/purchase-orders/99999999").header("Authorization","Bearer "+token)).andExpect(status().isNotFound());
    }
    @Test void supplierSpecificPricesAndAvailabilityAreEnforced() throws Exception {
        String token=login(Role.ADMIN);
        catalogue.put(supplierId,"PAINT",new BigDecimal("100.00"),true);
        long other=suppliers.saveAndFlush(new SupplierEntity("Other",null,null,null,null)).getId();
        mvc.perform(createRequest(token,valid().replace("\"supplierId\":"+supplierId,"\"supplierId\":"+other))).andExpect(status().is(422));
        mvc.perform(createRequest(token,valid())).andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(276.5));
        catalogue.put(supplierId,"PAINT",new BigDecimal("150.00"),false);
        mvc.perform(createRequest(token,valid())).andExpect(status().isConflict());
        assertEquals(1,orders.count());
    }
    @Test void catalogueReadAndAdminWritePermissions() throws Exception {
        String path="/api/suppliers/"+supplierId+"/products";
        String admin=login(Role.ADMIN), buyer=login(Role.PURCHASING_OFFICER);
        var c=csrf();
        mvc.perform(put(path+"/PAINT").cookie(c).header("X-XSRF-TOKEN",c.getValue()).header("Authorization","Bearer "+buyer)
            .contentType("application/json").content("{\"unitPrice\":100,\"active\":false}")).andExpect(status().isForbidden());
        mvc.perform(put(path+"/PAINT").cookie(c).header("X-XSRF-TOKEN",c.getValue()).header("Authorization","Bearer "+admin)
            .contentType("application/json").content("{\"unitPrice\":100,\"active\":false}")).andExpect(status().isOk());
        mvc.perform(get(path).header("Authorization","Bearer "+buyer)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get(path).param("available","false").header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
    }
}

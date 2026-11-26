package com.stockpurchaseorder.backend.supplier.persistence;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SupplierEntityTest {
    @Test void trimsDetailsAndDefaultsToActive() {
        var s = new SupplierEntity(" Hardware ", " Alice ", "", null, " Address ");
        assertEquals("Hardware", s.getName());
        assertEquals("Alice", s.getContactName());
        assertNull(s.getEmail());
        assertNull(s.getPhone());
        assertEquals("Address", s.getAddress());
        assertTrue(s.isActive());
    }
    @Test void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new SupplierEntity(" ", null, null, null, null));
    }
    @Test void updatesDetailsAndStatus() {
        var s = new SupplierEntity("Old", null, null, null, null);
        s.changeStatus(false);
        s.updateDetails("New", null, "sales@example.com", null, null);
        assertEquals("New", s.getName());
        assertEquals("sales@example.com", s.getEmail());
        assertFalse(s.isActive());
        s.changeStatus(true);
        assertTrue(s.isActive());
    }
}

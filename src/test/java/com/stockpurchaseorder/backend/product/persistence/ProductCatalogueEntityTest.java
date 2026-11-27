package com.stockpurchaseorder.backend.product.persistence;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class ProductCatalogueEntityTest {
    @Test void updatesDetailsWithoutChangingSku() {
        var product = new ProductEntity("PAINT-20L", "Paint", new BigDecimal("120.00"));
        product.updateDetails(" White paint ", new BigDecimal("125.50"));
        assertEquals("PAINT-20L", product.getSku());
        assertEquals("White paint", product.getName());
        assertEquals(new BigDecimal("125.50"), product.getUnitPrice());
    }

    @Test void rejectsInvalidPrice() {
        assertThrows(IllegalArgumentException.class, () -> new ProductEntity("A", "Paint", new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> new ProductEntity("A", "Paint", new BigDecimal("1.001")));
    }

    @Test void supportsSoftDeactivation() {
        var product = new ProductEntity("A", "Paint");
        assertTrue(product.isActive());
        product.changeStatus(false);
        assertFalse(product.isActive());
        product.changeStatus(true);
        assertTrue(product.isActive());
    }
}

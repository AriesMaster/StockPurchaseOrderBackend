package com.stockpurchaseorder.backend.product.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductEntityTest {

    @Test
    void createsProductWithAssignedSku() {
        ProductEntity product = new ProductEntity("PAINT-20L", "White paint");
        assertEquals("PAINT-20L", product.getSku());
        assertEquals("White paint", product.getName());
    }

    @Test
    void rejectsMissingSku() {
        assertThrows(IllegalArgumentException.class, () -> new ProductEntity(null, "Paint"));
        assertThrows(IllegalArgumentException.class, () -> new ProductEntity(" ", "Paint"));
    }

    @Test
    void rejectsMissingName() {
        assertThrows(IllegalArgumentException.class, () -> new ProductEntity("PAINT-20L", null));
        assertThrows(IllegalArgumentException.class, () -> new ProductEntity("PAINT-20L", " "));
    }
}

package com.stockpurchaseorder.backend.purchaseorder.domain;

import com.stockpurchaseorder.backend.common.exception.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DomainErrorTest {
    @Test void overDeliveryHasStableCode() {
        var order = new PurchaseOrder(List.of(new PurchaseOrderLine("A", 2)));
        order.approve();
        var error = assertThrows(DomainConflictException.class, () -> order.receive(Map.of("A", 3)));
        assertEquals(DomainErrorCode.QUANTITY_EXCEEDS_OUTSTANDING, error.getCode());
    }

    @Test void unknownSkuHasStableCode() {
        var order = new PurchaseOrder(List.of(new PurchaseOrderLine("A", 2)));
        order.approve();
        var error = assertThrows(DomainValidationException.class, () -> order.receive(Map.of("B", 1)));
        assertEquals(DomainErrorCode.SKU_NOT_IN_ORDER, error.getCode());
    }
}

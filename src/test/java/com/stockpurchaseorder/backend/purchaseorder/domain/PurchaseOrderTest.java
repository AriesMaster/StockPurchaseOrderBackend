package com.stockpurchaseorder.backend.purchaseorder.domain;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PurchaseOrderTest {
    private PurchaseOrder order() {
        return new PurchaseOrder(List.of(new PurchaseOrderLine("PAINT-20L", 20)));
    }

    @Test void startsAsDraftAndCanBeApproved() {
        var order = order();
        assertEquals(PurchaseOrderStatus.DRAFT, order.getStatus());
        order.approve();
        assertEquals(PurchaseOrderStatus.APPROVED, order.getStatus());
        assertThrows(IllegalStateException.class, order::approve);
    }

    @Test void receivesPartialThenCompleteDelivery() {
        var order = order();
        order.approve();
        order.receive(Map.of("PAINT-20L", 12));
        assertEquals(PurchaseOrderStatus.PARTIALLY_RECEIVED, order.getStatus());
        assertEquals(8, order.getLines().getFirst().getOutstandingQuantity());
        order.receive(Map.of("PAINT-20L", 8));
        assertEquals(PurchaseOrderStatus.RECEIVED, order.getStatus());
        assertEquals(0, order.getLines().getFirst().getOutstandingQuantity());
        assertThrows(IllegalStateException.class, () -> order.receive(Map.of("PAINT-20L", 1)));
    }

    @Test void rejectsOverDeliveryWithoutMutation() {
        var order = order();
        order.approve();
        order.receive(Map.of("PAINT-20L", 12));
        assertThrows(IllegalStateException.class, () -> order.receive(Map.of("PAINT-20L", 9)));
        assertEquals(12, order.getLines().getFirst().getReceivedQuantity());
        assertEquals(PurchaseOrderStatus.PARTIALLY_RECEIVED, order.getStatus());
    }

    @Test void validatesAllLinesBeforeChangingAny() {
        var order = new PurchaseOrder(List.of(new PurchaseOrderLine("A", 10), new PurchaseOrderLine("B", 5)));
        order.approve();
        assertThrows(IllegalStateException.class, () -> order.receive(Map.of("A", 3, "B", 6)));
        assertTrue(order.getLines().stream().allMatch(line -> line.getReceivedQuantity() == 0));
        assertEquals(PurchaseOrderStatus.APPROVED, order.getStatus());
    }

    @Test void rejectsDeliveryBeforeApprovalAndAfterCancellation() {
        var order = order();
        assertThrows(IllegalStateException.class, () -> order.receive(Map.of("PAINT-20L", 1)));
        order.cancel();
        assertEquals(PurchaseOrderStatus.CANCELLED, order.getStatus());
        assertThrows(IllegalStateException.class, order::approve);
        assertThrows(IllegalStateException.class, () -> order.receive(Map.of("PAINT-20L", 1)));
    }

    @Test void allowsCancellationBeforeReceiptButNotAfter() {
        var approved = order();
        approved.approve();
        approved.cancel();
        assertEquals(PurchaseOrderStatus.CANCELLED, approved.getStatus());
        var partial = order();
        partial.approve();
        partial.receive(Map.of("PAINT-20L", 1));
        assertThrows(IllegalStateException.class, partial::cancel);
    }

    @Test void rejectsInvalidOrderAndReceiptLines() {
        assertThrows(IllegalArgumentException.class, () -> new PurchaseOrder(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new PurchaseOrderLine("A", 0));
        assertThrows(IllegalArgumentException.class, () -> new PurchaseOrder(List.of(new PurchaseOrderLine("A", 1), new PurchaseOrderLine("A", 2))));
        var order = order();
        order.approve();
        assertThrows(IllegalArgumentException.class, () -> order.receive(Map.of("UNKNOWN", 1)));
        assertThrows(IllegalArgumentException.class, () -> order.receive(Map.of("PAINT-20L", 0)));
        assertThrows(IllegalArgumentException.class, () -> order.receive(Map.of()));
    }
}

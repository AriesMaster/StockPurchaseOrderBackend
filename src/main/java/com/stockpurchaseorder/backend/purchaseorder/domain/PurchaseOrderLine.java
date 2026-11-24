package com.stockpurchaseorder.backend.purchaseorder.domain;

import com.stockpurchaseorder.backend.common.exception.*;

public final class PurchaseOrderLine {
    private final String sku;
    private final int orderedQuantity;
    private int receivedQuantity;

    public PurchaseOrderLine(String sku, int orderedQuantity) {
        if (sku == null || sku.isBlank() || orderedQuantity <= 0) {
            throw new DomainValidationException(DomainErrorCode.INVALID_ORDER_LINE, "SKU and positive quantity are required");
        }
        this.sku = sku;
        this.orderedQuantity = orderedQuantity;
    }

    public String getSku() { return sku; }
    public int getOrderedQuantity() { return orderedQuantity; }
    public int getReceivedQuantity() { return receivedQuantity; }
    public int getOutstandingQuantity() { return orderedQuantity - receivedQuantity; }

    void receive(int quantity) {
        if (quantity <= 0 || quantity > getOutstandingQuantity()) {
            throw new DomainValidationException(DomainErrorCode.INVALID_RECEIPT_QUANTITY, "Invalid receipt quantity");
        }
        receivedQuantity += quantity;
    }

    PurchaseOrderLine copy() {
        var copy = new PurchaseOrderLine(sku, orderedQuantity);
        copy.receivedQuantity = receivedQuantity;
        return copy;
    }
}

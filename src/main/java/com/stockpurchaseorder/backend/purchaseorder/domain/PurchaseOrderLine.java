package com.stockpurchaseorder.backend.purchaseorder.domain;

public final class PurchaseOrderLine {
    private final String sku;
    private final int orderedQuantity;
    private int receivedQuantity;

    public PurchaseOrderLine(String sku, int orderedQuantity) {
        if (sku == null || sku.isBlank() || orderedQuantity <= 0) {
            throw new IllegalArgumentException("SKU and positive quantity are required");
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
            throw new IllegalArgumentException("Invalid receipt quantity");
        }
        receivedQuantity += quantity;
    }

    PurchaseOrderLine copy() {
        var copy = new PurchaseOrderLine(sku, orderedQuantity);
        copy.receivedQuantity = receivedQuantity;
        return copy;
    }
}

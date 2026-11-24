package com.stockpurchaseorder.backend.purchaseorder.domain;

import com.stockpurchaseorder.backend.common.exception.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PurchaseOrder {
    private final Map<String, PurchaseOrderLine> lines = new LinkedHashMap<>();
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    public PurchaseOrder(List<PurchaseOrderLine> orderLines) {
        if (orderLines == null || orderLines.isEmpty()) {
            throw new DomainValidationException(DomainErrorCode.EMPTY_ORDER, "At least one order line is required");
        }
        for (var line : orderLines) {
            if (line == null || lines.putIfAbsent(line.getSku(),
                    new PurchaseOrderLine(line.getSku(), line.getOrderedQuantity())) != null) {
                throw new DomainValidationException(DomainErrorCode.DUPLICATE_SKU, "Order lines must have unique SKUs");
            }
        }
    }

    public PurchaseOrderStatus getStatus() { return status; }

    public List<PurchaseOrderLine> getLines() {
        return lines.values().stream().map(PurchaseOrderLine::copy).toList();
    }

    public void approve() {
        if (status != PurchaseOrderStatus.DRAFT) {
            throw new DomainConflictException(DomainErrorCode.INVALID_ORDER_STATUS, "Only draft orders can be approved");
        }
        status = PurchaseOrderStatus.APPROVED;
    }

    public void cancel() {
        if (status != PurchaseOrderStatus.DRAFT && status != PurchaseOrderStatus.APPROVED) {
            throw new DomainConflictException(DomainErrorCode.INVALID_ORDER_STATUS, "Order cannot be cancelled");
        }
        status = PurchaseOrderStatus.CANCELLED;
    }

    public void receive(Map<String, Integer> quantities) {
        if (status != PurchaseOrderStatus.APPROVED && status != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new DomainConflictException(DomainErrorCode.INVALID_ORDER_STATUS, "Order cannot accept deliveries");
        }
        if (quantities == null || quantities.isEmpty()) {
            throw new DomainValidationException(DomainErrorCode.EMPTY_RECEIPT, "At least one receipt line is required");
        }
        // Validate every line before mutating any quantity.
        var receipt = new LinkedHashMap<>(quantities);
        receipt.forEach((sku, quantity) -> {
            var line = lines.get(sku);
            if (line == null) {
                throw new DomainValidationException(DomainErrorCode.SKU_NOT_IN_ORDER, "SKU is not in order");
            }
            if (quantity == null || quantity <= 0) {
                throw new DomainValidationException(DomainErrorCode.INVALID_RECEIPT_QUANTITY, "Invalid receipt quantity");
            }
            if (quantity > line.getOutstandingQuantity()) {
                throw new DomainConflictException(DomainErrorCode.QUANTITY_EXCEEDS_OUTSTANDING, "Quantity exceeds outstanding stock");
            }
        });
        receipt.forEach((sku, quantity) -> lines.get(sku).receive(quantity));
        status = lines.values().stream().allMatch(line -> line.getOutstandingQuantity() == 0)
                ? PurchaseOrderStatus.RECEIVED : PurchaseOrderStatus.PARTIALLY_RECEIVED;
    }
}

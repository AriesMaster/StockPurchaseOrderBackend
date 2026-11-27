package com.stockpurchaseorder.backend.purchaseorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record CreatePurchaseOrderRequest(@NotNull @Positive Long supplierId,
    @NotEmpty @Size(max=100) List<@NotNull @Valid Line> lines) {
    public record Line(@NotBlank @Size(max=80) String sku, @NotNull @Positive Integer orderedQuantity) {}
}

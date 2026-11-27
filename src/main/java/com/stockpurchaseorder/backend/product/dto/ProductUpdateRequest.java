package com.stockpurchaseorder.backend.product.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductUpdateRequest(
    @NotBlank @Size(max=150) String name,
    @NotNull @DecimalMin("0.00") @Digits(integer=10, fraction=2) BigDecimal unitPrice
) {}

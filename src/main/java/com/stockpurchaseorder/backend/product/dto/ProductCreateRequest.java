package com.stockpurchaseorder.backend.product.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductCreateRequest(
    @NotBlank @Size(max=80) @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9._-]*") String sku,
    @NotBlank @Size(max=150) String name,
    @NotNull @DecimalMin("0.00") @Digits(integer=10, fraction=2) BigDecimal unitPrice
) {}

package com.stockpurchaseorder.backend.supplier.dto;
import jakarta.validation.constraints.*;
public record SupplierRequest(@NotBlank @Size(max=150) String name,
    @Size(max=150) String contactName, @Email @Size(max=254) String email,
    @Size(max=30) String phone, @Size(max=500) String address) {}

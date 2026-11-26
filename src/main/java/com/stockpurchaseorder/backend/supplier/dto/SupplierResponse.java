package com.stockpurchaseorder.backend.supplier.dto;
import java.time.Instant;
import com.stockpurchaseorder.backend.supplier.persistence.SupplierEntity;
public record SupplierResponse(Long id, String name, String contactName, String email,
    String phone, String address, boolean active, Instant createdAt, Instant updatedAt) {
    public static SupplierResponse from(SupplierEntity s) {
        return new SupplierResponse(s.getId(), s.getName(), s.getContactName(), s.getEmail(),
            s.getPhone(), s.getAddress(), s.isActive(), s.getCreatedAt(), s.getUpdatedAt());
    }
}

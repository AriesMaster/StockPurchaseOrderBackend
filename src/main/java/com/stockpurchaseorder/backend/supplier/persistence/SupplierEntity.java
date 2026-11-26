package com.stockpurchaseorder.backend.supplier.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "supplier")
public class SupplierEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(name = "contact_name", length = 150)
    private String contactName;
    @Column(length = 254)
    private String email;
    @Column(length = 30)
    private String phone;
    @Column(length = 500)
    private String address;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SupplierEntity() {}

    public SupplierEntity(String name, String contactName, String email, String phone, String address) {
        updateDetails(name, contactName, email, phone, address);
    }

    public void updateDetails(String name, String contactName, String email, String phone, String address) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Supplier name must not be blank.");
        this.name = name.strip();
        this.contactName = optional(contactName);
        this.email = optional(email);
        this.phone = optional(phone);
        this.address = optional(address);
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public void changeStatus(boolean active) { this.active = active; }
    @PrePersist
    private void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate
    private void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getContactName() { return contactName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

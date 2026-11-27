package com.stockpurchaseorder.backend.supplier.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupplierRepository extends JpaRepository<SupplierEntity, Long> {
    Page<SupplierEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<SupplierEntity> findByActive(boolean active, Pageable pageable);
    Page<SupplierEntity> findByActiveAndNameContainingIgnoreCase(boolean active, String name, Pageable pageable);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_READ)
    @org.springframework.data.jpa.repository.Query("select e from SupplierEntity e where e.id = :key")
    java.util.Optional<SupplierEntity> lockForPurchase(@org.springframework.data.repository.query.Param("key") Long key);
}

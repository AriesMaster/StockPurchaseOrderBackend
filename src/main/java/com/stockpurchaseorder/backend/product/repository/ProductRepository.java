package com.stockpurchaseorder.backend.product.repository;

import com.stockpurchaseorder.backend.product.persistence.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<ProductEntity, String> {
    Page<ProductEntity> findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(String name, String sku, Pageable pageable);
    Page<ProductEntity> findByActive(boolean active, Pageable pageable);
    Page<ProductEntity> findByActiveAndNameContainingIgnoreCaseOrActiveAndSkuContainingIgnoreCase(boolean active, String name, boolean sameActive, String sku, Pageable pageable);
}

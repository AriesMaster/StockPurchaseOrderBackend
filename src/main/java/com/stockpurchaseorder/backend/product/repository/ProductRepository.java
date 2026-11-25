package com.stockpurchaseorder.backend.product.repository;

import com.stockpurchaseorder.backend.product.persistence.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<ProductEntity, String> {
}

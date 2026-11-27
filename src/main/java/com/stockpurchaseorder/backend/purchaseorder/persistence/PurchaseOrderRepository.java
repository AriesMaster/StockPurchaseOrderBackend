package com.stockpurchaseorder.backend.purchaseorder.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.stockpurchaseorder.backend.purchaseorder.domain.PurchaseOrderStatus;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrderEntity,Long> {
    Page<PurchaseOrderEntity> findByStatus(PurchaseOrderStatus status, Pageable pageable);
}

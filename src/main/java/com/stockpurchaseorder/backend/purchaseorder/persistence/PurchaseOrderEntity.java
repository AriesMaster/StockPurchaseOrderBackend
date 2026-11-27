package com.stockpurchaseorder.backend.purchaseorder.persistence;

import com.stockpurchaseorder.backend.purchaseorder.domain.PurchaseOrderStatus;
import com.stockpurchaseorder.backend.supplier.persistence.SupplierEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name="purchase_order")
public class PurchaseOrderEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="supplier_id", nullable=false, updatable=false)
    private SupplierEntity supplier;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false, columnDefinition="text")
    private PurchaseOrderStatus status=PurchaseOrderStatus.DRAFT;
    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt=Instant.now();

    @ElementCollection
    @CollectionTable(name="purchase_order_line", joinColumns=@JoinColumn(name="purchase_order_id"))
    @MapKeyColumn(name="sku", nullable=false, columnDefinition="text")
    @org.hibernate.annotations.BatchSize(size=20)
    private Map<String, OrderLineSnapshot> lines=new LinkedHashMap<>();

    protected PurchaseOrderEntity() {}
    public PurchaseOrderEntity(SupplierEntity supplier, Map<String, OrderLineSnapshot> lines) {
        if (lines.isEmpty()) throw new IllegalArgumentException("Order must contain lines");
        this.supplier=supplier;
        this.lines.putAll(lines);
    }
    public Long getId(){return id;}
    public SupplierEntity getSupplier(){return supplier;}
    public PurchaseOrderStatus getStatus(){return status;}
    public Instant getCreatedAt(){return createdAt;}
    public Map<String, OrderLineSnapshot> getLines(){return java.util.Collections.unmodifiableMap(lines);}
}

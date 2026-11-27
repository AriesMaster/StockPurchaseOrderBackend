package com.stockpurchaseorder.backend.supplier.catalogue;

import com.stockpurchaseorder.backend.supplier.persistence.SupplierEntity;
import com.stockpurchaseorder.backend.product.persistence.ProductEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name="supplier_product",uniqueConstraints=@UniqueConstraint(columnNames={"supplier_id","sku"}))
public class SupplierProduct {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="supplier_id",nullable=false,updatable=false)
    private SupplierEntity supplier;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="sku",nullable=false,updatable=false)
    private ProductEntity product;
    @Column(name="unit_price",nullable=false,precision=12,scale=2)
    private BigDecimal unitPrice;
    @Column(nullable=false)
    private boolean active;
    protected SupplierProduct(){}
    public SupplierProduct(SupplierEntity supplier,ProductEntity product,BigDecimal price,boolean active){
        this.supplier=supplier;this.product=product;update(price,active);
    }
    public void update(BigDecimal price,boolean active){this.unitPrice=price;this.active=active;}
    public ProductEntity getProduct(){return product;}
    public BigDecimal getUnitPrice(){return unitPrice;}
    public boolean isActive(){return active;}
}

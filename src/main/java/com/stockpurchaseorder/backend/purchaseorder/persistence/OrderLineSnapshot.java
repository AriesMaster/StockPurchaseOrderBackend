package com.stockpurchaseorder.backend.purchaseorder.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Embeddable
public class OrderLineSnapshot {
    @Column(name="product_name", nullable=false, columnDefinition="text")
    private String productName;
    @Column(name="ordered_quantity", nullable=false)
    private int orderedQuantity;
    @Column(name="received_quantity", nullable=false)
    private int receivedQuantity;
    @Column(name="unit_price", nullable=false, precision=12, scale=2)
    private BigDecimal unitPrice;

    protected OrderLineSnapshot() {}
    public OrderLineSnapshot(String productName, int orderedQuantity, BigDecimal unitPrice) {
        this.productName=productName;
        this.orderedQuantity=orderedQuantity;
        this.unitPrice=unitPrice;
    }
    public String getProductName(){return productName;}
    public int getOrderedQuantity(){return orderedQuantity;}
    public int getReceivedQuantity(){return receivedQuantity;}
    public BigDecimal getUnitPrice(){return unitPrice;}
}

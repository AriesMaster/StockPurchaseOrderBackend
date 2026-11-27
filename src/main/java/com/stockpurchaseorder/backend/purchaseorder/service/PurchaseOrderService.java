package com.stockpurchaseorder.backend.purchaseorder.service;

import com.stockpurchaseorder.backend.purchaseorder.domain.*;
import com.stockpurchaseorder.backend.purchaseorder.dto.*;
import com.stockpurchaseorder.backend.purchaseorder.persistence.*;
import com.stockpurchaseorder.backend.product.repository.ProductRepository;
import com.stockpurchaseorder.backend.supplier.persistence.SupplierRepository;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.LinkedHashMap;
import java.util.Comparator;

@Service
public class PurchaseOrderService {
    private final PurchaseOrderRepository orders;
    private final SupplierRepository suppliers;
    private final ProductRepository products;
    public PurchaseOrderService(PurchaseOrderRepository orders, SupplierRepository suppliers, ProductRepository products) {
        this.orders=orders;this.suppliers=suppliers;this.products=products;
    }

    @Transactional
    public PurchaseOrderResponse create(CreatePurchaseOrderRequest request) {
        // Reuse the existing domain's duplicate-SKU and quantity rules before persistence.
        var draft=new PurchaseOrder(request.lines().stream()
            .map(line -> new PurchaseOrderLine(line.sku(),line.orderedQuantity())).toList());
        var supplier=suppliers.lockForPurchase(request.supplierId()).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,"Supplier not found"));
        if(!supplier.isActive()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Supplier is inactive");
        var snapshots=new LinkedHashMap<String,OrderLineSnapshot>();
        // Lock catalogue rows in stable SKU order; prevent deactivation or price changes during creation.
        for(var line:draft.getLines().stream().sorted(Comparator.comparing(PurchaseOrderLine::getSku)).toList()){
            var product=products.lockForPurchase(line.getSku()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,"Unknown SKU: "+line.getSku()));
            if(!product.isActive()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Inactive SKU: "+line.getSku());
            snapshots.put(line.getSku(),new OrderLineSnapshot(product.getName(),line.getOrderedQuantity(),product.getUnitPrice()));
        }
        return PurchaseOrderResponse.from(orders.saveAndFlush(new PurchaseOrderEntity(supplier,snapshots)));
    }

    @Transactional(readOnly=true)
    public PurchaseOrderResponse get(long id){
        return PurchaseOrderResponse.from(orders.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND,"Purchase order not found")));
    }

    @Transactional(readOnly=true)
    public Page<PurchaseOrderResponse> list(int page,int size,PurchaseOrderStatus status){
        if(page<0||size<1||size>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid pagination");
        var pageable=PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt","id"));
        return (status==null?orders.findAll(pageable):orders.findByStatus(status,pageable)).map(PurchaseOrderResponse::from);
    }
}

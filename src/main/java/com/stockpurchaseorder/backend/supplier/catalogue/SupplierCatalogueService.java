package com.stockpurchaseorder.backend.supplier.catalogue;

import com.stockpurchaseorder.backend.supplier.persistence.SupplierRepository;
import com.stockpurchaseorder.backend.product.repository.ProductRepository;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;

@Service
public class SupplierCatalogueService {
    private final SupplierProductRepository catalogue;
    private final SupplierRepository suppliers;
    private final ProductRepository products;
    public SupplierCatalogueService(SupplierProductRepository catalogue,SupplierRepository suppliers,ProductRepository products){
        this.catalogue=catalogue;this.suppliers=suppliers;this.products=products;
    }
    public record Entry(String sku,String name,BigDecimal unitPrice,boolean active,boolean productActive){
        static Entry from(SupplierProduct link){return new Entry(link.getProduct().getSku(),link.getProduct().getName(),link.getUnitPrice(),link.isActive(),link.getProduct().isActive());}
    }
    @Transactional(readOnly=true)
    public Page<Entry> list(long id,boolean available,String search,int page,int size){
        if(page<0||size<1||size>100||search.length()>150)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid catalogue query");
        if(!suppliers.existsById(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Supplier not found");
        return catalogue.catalogue(id,available,search.strip(),PageRequest.of(page,size)).map(Entry::from);
    }
    @Transactional
    public Entry put(long id,String sku,BigDecimal price,boolean active){
        // Exclusive supplier lock serializes assignments and changes against order creation.
        var supplier=suppliers.lockForCatalogue(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Supplier not found"));
        var product=products.lockForPurchase(sku).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found"));
        if(active&&(!supplier.isActive()||!product.isActive()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Supplier and product must be active");
        var link=catalogue.findBySupplier_IdAndProduct_Sku(id,sku).orElseGet(()->new SupplierProduct(supplier,product,price,active));
        link.update(price,active);
        return Entry.from(catalogue.saveAndFlush(link));
    }
}

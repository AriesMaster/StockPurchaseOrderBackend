package com.stockpurchaseorder.backend.product.service;

import com.stockpurchaseorder.backend.product.dto.*;
import com.stockpurchaseorder.backend.product.persistence.ProductEntity;
import com.stockpurchaseorder.backend.product.repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {
    private final ProductRepository products;
    public ProductService(ProductRepository products) { this.products = products; }

    @Transactional(readOnly = true)
    public Page<ProductResponse> list(int page, int size, String search) {
        if (page < 0 || size < 1 || size > 100 || (search != null && search.length() > 150))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination or search");
        var pageable = PageRequest.of(page, size, Sort.by("name", "sku"));
        return (search == null || search.isBlank() ? products.findAll(pageable) :
            products.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(search.strip(), search.strip(), pageable))
            .map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(String sku) { return ProductResponse.from(require(sku)); }

    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        if (products.existsById(request.sku())) throw duplicate();
        try {
            return ProductResponse.from(products.saveAndFlush(
                new ProductEntity(request.sku(), request.name(), request.unitPrice())));
        } catch (DataIntegrityViolationException ex) {
            // The primary key also rejects simultaneous attempts to create the same SKU.
            throw duplicate();
        }
    }

    @Transactional
    public ProductResponse update(String sku, ProductUpdateRequest request) {
        var product = require(sku);
        product.updateDetails(request.name(), request.unitPrice());
        return persist(product);
    }

    @Transactional
    public ProductResponse changeStatus(String sku, boolean active) {
        var product = require(sku);
        product.changeStatus(active);
        return persist(product);
    }

    private ProductResponse persist(ProductEntity product) {
        try {
            return ProductResponse.from(products.saveAndFlush(product));
        } catch (OptimisticLockingFailureException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Product was changed concurrently; reload and retry");
        }
    }

    private ProductEntity require(String sku) {
        return products.findById(sku).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private ResponseStatusException duplicate() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "SKU already exists");
    }
}

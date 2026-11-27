package com.stockpurchaseorder.backend.product.controller;

import com.stockpurchaseorder.backend.common.dto.PageResponse;
import com.stockpurchaseorder.backend.product.dto.*;
import com.stockpurchaseorder.backend.product.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService products;
    public ProductController(ProductService products) { this.products = products; }

    @GetMapping
    public PageResponse<ProductResponse> list(@RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="20") int size, @RequestParam(required=false) String search, @RequestParam(required=false) Boolean active) {
        return PageResponse.from(products.list(page, size, search, active));
    }

    @GetMapping("/{sku}")
    public ProductResponse get(@PathVariable String sku) { return products.get(sku); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductCreateRequest request) { return products.create(request); }

    @PutMapping("/{sku}")
    public ProductResponse update(@PathVariable String sku, @Valid @RequestBody ProductUpdateRequest request) {
        return products.update(sku, request);
    }

    public record StatusRequest(@NotNull Boolean active) {}

    @PatchMapping("/{sku}/status")
    public ProductResponse changeStatus(@PathVariable String sku, @Valid @RequestBody StatusRequest request) {
        return products.changeStatus(sku, request.active());
    }
}

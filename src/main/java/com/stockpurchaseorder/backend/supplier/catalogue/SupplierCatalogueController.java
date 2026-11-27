package com.stockpurchaseorder.backend.supplier.catalogue;

import com.stockpurchaseorder.backend.common.dto.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/suppliers/{id}/products")
public class SupplierCatalogueController {
    private final SupplierCatalogueService service;
    public SupplierCatalogueController(SupplierCatalogueService service){this.service=service;}
    public record Request(@NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal unitPrice,@NotNull Boolean active){}
    @GetMapping
    public PageResponse<SupplierCatalogueService.Entry> list(@PathVariable long id,@RequestParam(defaultValue="true") boolean available,
        @RequestParam(defaultValue="") String search,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size){
        return PageResponse.from(service.list(id,available,search,page,size));
    }
    @PutMapping("/{sku}")
    public SupplierCatalogueService.Entry put(@PathVariable long id,@PathVariable String sku,@Valid @RequestBody Request request){
        return service.put(id,sku,request.unitPrice(),request.active());
    }
}

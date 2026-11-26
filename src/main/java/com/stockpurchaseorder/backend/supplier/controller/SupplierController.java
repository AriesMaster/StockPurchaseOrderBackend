package com.stockpurchaseorder.backend.supplier.controller;
import com.stockpurchaseorder.backend.supplier.dto.*;
import com.stockpurchaseorder.backend.supplier.service.SupplierService;
import com.stockpurchaseorder.backend.common.dto.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {
    private final SupplierService suppliers;
    public SupplierController(SupplierService suppliers) { this.suppliers = suppliers; }
    @GetMapping
    public PageResponse<SupplierResponse> list(@RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="20") int size, @RequestParam(required=false) String search) {
        return PageResponse.from(suppliers.list(page, size, search));
    }
    @GetMapping("/{id}")
    public SupplierResponse get(@PathVariable long id) { return suppliers.get(id); }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierResponse create(@Valid @RequestBody SupplierRequest request) { return suppliers.create(request); }
    @PutMapping("/{id}")
    public SupplierResponse update(@PathVariable long id, @Valid @RequestBody SupplierRequest request) {
        return suppliers.update(id, request);
    }
    public record StatusRequest(@NotNull Boolean active) {}
    @PatchMapping("/{id}/status")
    public SupplierResponse changeStatus(@PathVariable long id, @Valid @RequestBody StatusRequest request) {
        return suppliers.changeStatus(id, request.active());
    }
}

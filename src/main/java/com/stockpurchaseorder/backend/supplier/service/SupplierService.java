package com.stockpurchaseorder.backend.supplier.service;
import com.stockpurchaseorder.backend.supplier.dto.*;
import com.stockpurchaseorder.backend.supplier.persistence.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SupplierService {
    private final SupplierRepository suppliers;
    public SupplierService(SupplierRepository suppliers) { this.suppliers = suppliers; }

    @Transactional(readOnly=true)
    public Page<SupplierResponse> list(int page, int size, String search) {
        return list(page, size, search, null);
    }

    @Transactional(readOnly=true)
    public Page<SupplierResponse> list(int page, int size, String search, Boolean active) {
        if (page < 0 || size < 1 || size > 100 || (search != null && search.length() > 150))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination or search");
        var pageable = PageRequest.of(page, size, Sort.by("name", "id"));
        if (active != null) {
            return (search == null || search.isBlank() ? suppliers.findByActive(active, pageable)
                : suppliers.findByActiveAndNameContainingIgnoreCase(active, search.strip(), pageable)).map(SupplierResponse::from);
        }
        return (search == null || search.isBlank() ? suppliers.findAll(pageable)
            : suppliers.findByNameContainingIgnoreCase(search.strip(), pageable)).map(SupplierResponse::from);
    }
    @Transactional(readOnly=true)
    public SupplierResponse get(long id) { return SupplierResponse.from(require(id)); }
    @Transactional
    public SupplierResponse create(SupplierRequest r) {
        return SupplierResponse.from(suppliers.saveAndFlush(new SupplierEntity(r.name(), r.contactName(), r.email(), r.phone(), r.address())));
    }
    @Transactional
    public SupplierResponse update(long id, SupplierRequest r) {
        var s = require(id);
        s.updateDetails(r.name(), r.contactName(), r.email(), r.phone(), r.address());
        return SupplierResponse.from(suppliers.saveAndFlush(s));
    }
    @Transactional
    public SupplierResponse changeStatus(long id, boolean active) {
        var s = require(id);
        s.changeStatus(active);
        return SupplierResponse.from(suppliers.saveAndFlush(s));
    }
    private SupplierEntity require(long id) {
        return suppliers.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));
    }
}

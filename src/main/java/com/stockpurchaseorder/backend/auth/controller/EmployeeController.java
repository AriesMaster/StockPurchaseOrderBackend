package com.stockpurchaseorder.backend.auth.controller;

import com.stockpurchaseorder.backend.auth.dto.*;
import com.stockpurchaseorder.backend.auth.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    private final EmployeeService employees;

    public EmployeeController(EmployeeService employees) { this.employees = employees; }

    @GetMapping
    public Page<EmployeeResponse> list(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return employees.list(page, size);
    }

    @GetMapping("/{id}")
    public EmployeeResponse get(@PathVariable long id) { return employees.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse create(@Valid @RequestBody CreateEmployeeRequest request) {
        return employees.create(request);
    }
}

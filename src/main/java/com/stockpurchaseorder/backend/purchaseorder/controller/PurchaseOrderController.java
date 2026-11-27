package com.stockpurchaseorder.backend.purchaseorder.controller;

import com.stockpurchaseorder.backend.common.dto.PageResponse;
import com.stockpurchaseorder.backend.purchaseorder.domain.PurchaseOrderStatus;
import com.stockpurchaseorder.backend.purchaseorder.dto.*;
import com.stockpurchaseorder.backend.purchaseorder.service.PurchaseOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {
    private final PurchaseOrderService orders;
    public PurchaseOrderController(PurchaseOrderService orders){this.orders=orders;}

    @GetMapping
    public PageResponse<PurchaseOrderResponse> list(@RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="20") int size,@RequestParam(required=false) PurchaseOrderStatus status){
        return PageResponse.from(orders.list(page,size,status));
    }
    @GetMapping("/{id}")
    public PurchaseOrderResponse get(@PathVariable long id){return orders.get(id);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public PurchaseOrderResponse create(@Valid @RequestBody CreatePurchaseOrderRequest request){return orders.create(request);}
}

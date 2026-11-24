package com.stockpurchaseorder.backend.common.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test void invalidQuantityReturnsBadRequest() {
        var problem = handler.handleValidation(new DomainValidationException(
                DomainErrorCode.INVALID_RECEIPT_QUANTITY, "Invalid receipt quantity"));
        assertEquals(400, problem.getStatus());
        assertEquals("INVALID_RECEIPT_QUANTITY", problem.getProperties().get("code"));
    }

    @Test void unknownSkuReturnsUnprocessableEntity() {
        var problem = handler.handleValidation(new DomainValidationException(
                DomainErrorCode.SKU_NOT_IN_ORDER, "SKU is not in order"));
        assertEquals(422, problem.getStatus());
        assertEquals("SKU_NOT_IN_ORDER", problem.getProperties().get("code"));
    }

    @Test void overDeliveryReturnsConflict() {
        var problem = handler.handleConflict(new DomainConflictException(
                DomainErrorCode.QUANTITY_EXCEEDS_OUTSTANDING, "Quantity exceeds outstanding stock"));
        assertEquals(409, problem.getStatus());
        assertEquals("QUANTITY_EXCEEDS_OUTSTANDING", problem.getProperties().get("code"));
        assertEquals("Quantity exceeds outstanding stock", problem.getDetail());
    }
}

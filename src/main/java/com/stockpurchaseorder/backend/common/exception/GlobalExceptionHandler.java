package com.stockpurchaseorder.backend.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainValidationException.class)
    public ProblemDetail handleValidation(DomainValidationException exception) {
        HttpStatus status = exception.getCode() == DomainErrorCode.SKU_NOT_IN_ORDER
                ? HttpStatus.UNPROCESSABLE_CONTENT : HttpStatus.BAD_REQUEST;
        return problem(status, exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(DomainConflictException.class)
    public ProblemDetail handleConflict(DomainConflictException exception) {
        return problem(HttpStatus.CONFLICT, exception.getCode(), exception.getMessage());
    }

    private ProblemDetail problem(HttpStatus status, DomainErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code.name());
        return problem;
    }
}

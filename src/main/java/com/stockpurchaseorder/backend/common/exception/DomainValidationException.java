package com.stockpurchaseorder.backend.common.exception;

public final class DomainValidationException extends IllegalArgumentException {
    private final DomainErrorCode code;

    public DomainValidationException(DomainErrorCode code, String message) {
        super(message);
        this.code = java.util.Objects.requireNonNull(code);
    }

    public DomainErrorCode getCode() { return code; }
}

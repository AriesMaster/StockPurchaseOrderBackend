package com.stockpurchaseorder.backend.common.exception;

public final class DomainConflictException extends IllegalStateException {
    private final DomainErrorCode code;

    public DomainConflictException(DomainErrorCode code, String message) {
        super(message);
        this.code = java.util.Objects.requireNonNull(code);
    }

    public DomainErrorCode getCode() { return code; }
}

package com.springboot.erp.platform.error;

import org.springframework.http.HttpStatus;

/**
 * Stable, catalogued error codes (ARCHITECTURE.md §2 — errors as RFC-7807 with
 * a stable code). The {@code code} string is part of the API contract and must
 * never change once shipped; add new codes rather than repurposing old ones.
 * Documented in {@code /docs/errors.md}.
 */
public enum ErrorCode {

    // --- Generic ---
    VALIDATION_FAILED("validation-failed", HttpStatus.BAD_REQUEST, "Request validation failed"),
    NOT_FOUND("not-found", HttpStatus.NOT_FOUND, "Resource not found"),
    CONFLICT("conflict", HttpStatus.CONFLICT, "Conflict"),
    OPTIMISTIC_LOCK("optimistic-lock", HttpStatus.CONFLICT, "The record was modified concurrently"),
    DUPLICATE("duplicate", HttpStatus.CONFLICT, "A record with these values already exists"),

    // --- Auth ---
    UNAUTHENTICATED("unauthenticated", HttpStatus.UNAUTHORIZED, "Authentication required"),
    INVALID_CREDENTIALS("invalid-credentials", HttpStatus.UNAUTHORIZED, "Invalid username or password"),
    TOKEN_EXPIRED("token-expired", HttpStatus.UNAUTHORIZED, "Token expired"),
    FORBIDDEN("forbidden", HttpStatus.FORBIDDEN, "You do not have permission to perform this action"),

    // --- Domain / state ---
    ILLEGAL_STATE_TRANSITION("illegal-state-transition", HttpStatus.CONFLICT, "Illegal status transition"),
    REFERENCED("referenced", HttpStatus.CONFLICT, "Cannot delete: the record is still referenced"),

    // --- Fallback ---
    INTERNAL("internal", HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");

    private final String code;
    private final HttpStatus status;
    private final String defaultDetail;

    ErrorCode(String code, HttpStatus status, String defaultDetail) {
        this.code = code;
        this.status = status;
        this.defaultDetail = defaultDetail;
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultDetail() {
        return defaultDetail;
    }
}

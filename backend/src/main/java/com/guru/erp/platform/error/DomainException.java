package com.guru.erp.platform.error;

import java.util.Map;

/**
 * Base for all business-rule failures. Carries a catalogued {@link ErrorCode}
 * (→ HTTP status + stable code) and optional structured properties that the
 * advice copies into the RFC-7807 {@code ProblemDetail}.
 */
public class DomainException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Map<String, Object> properties;

    public DomainException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultDetail(), Map.of());
    }

    public DomainException(ErrorCode errorCode, String detail) {
        this(errorCode, detail, Map.of());
    }

    public DomainException(ErrorCode errorCode, String detail, Map<String, Object> properties) {
        super(detail);
        this.errorCode = errorCode;
        this.properties = properties == null ? Map.of() : Map.copyOf(properties);
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public Map<String, Object> properties() {
        return properties;
    }

    // --- Convenience factories for the most common cases ---

    public static DomainException notFound(String resource, String id) {
        return new DomainException(
            ErrorCode.NOT_FOUND,
            "%s '%s' was not found".formatted(resource, id),
            Map.of("resource", resource, "id", id));
    }

    public static DomainException forbidden(String permission) {
        return new DomainException(
            ErrorCode.FORBIDDEN,
            "Missing required permission: " + permission,
            Map.of("requiredPermission", permission));
    }

    public static DomainException illegalTransition(String from, String to) {
        return new DomainException(
            ErrorCode.ILLEGAL_STATE_TRANSITION,
            "Cannot transition from '%s' to '%s'".formatted(from, to),
            Map.of("from", from, "to", to));
    }
}

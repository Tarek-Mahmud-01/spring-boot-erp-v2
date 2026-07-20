package com.springboot.erp.modules.settings.currency.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Request/response DTOs for ENT-018 Currency (ARCHITECTURE.md §2 — records).
 * Field constraints mirror the reference Pydantic schemas.
 */
public final class CurrencyDtos {

    private CurrencyDtos() {
    }

    /**
     * Create payload. {@code code} must be exactly 3 alphabetic ISO-4217
     * characters; the service upper-cases it before persisting.
     */
    public record CurrencyCreateRequest(
        @NotBlank @Size(min = 3, max = 3)
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "code must be 3 alphabetic ISO 4217 characters")
        String code,

        @NotBlank @Size(min = 1, max = 100) String name,
        @NotBlank @Size(min = 1, max = 20) String shortName,
        @NotBlank @Size(min = 1, max = 100) String country,
        @NotBlank @Size(min = 1, max = 8) String symbol,

        @Min(0) @Max(4) Integer decimalPlaces,
        Boolean isActive
    ) {
    }

    /**
     * Partial update. {@code code} and {@code isDefault} are intentionally
     * omitted — {@code code} is immutable and promotion happens via
     * {@code POST /{publicId}/set-default}. {@code version} carries the client's
     * expected optimistic-lock value.
     */
    public record CurrencyUpdateRequest(
        @Size(min = 1, max = 100) String name,
        @Size(min = 1, max = 20) String shortName,
        @Size(min = 1, max = 100) String country,
        @Size(min = 1, max = 8) String symbol,
        @Min(0) @Max(4) Integer decimalPlaces,
        Boolean isActive,
        Long version
    ) {
    }

    /** Read shape. {@code id} is the ULID publicId; {@code status} is derived. */
    public record CurrencyResponse(
        String id,
        String code,
        String name,
        String shortName,
        String country,
        String symbol,
        int decimalPlaces,
        boolean isDefault,
        boolean isActive,
        String status,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {
    }
}

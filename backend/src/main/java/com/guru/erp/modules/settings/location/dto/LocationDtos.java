package com.guru.erp.modules.settings.location.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Wire records for the location endpoints (ARCHITECTURE.md §2 — DTOs are records
 * with jakarta-validation constraints mirroring the reference Pydantic fields).
 *
 * <p>Format-level checks live here; cross-field business rules (IANA timezone
 * validity, type-enum membership, per-company code uniqueness, optimistic-lock)
 * are enforced in {@code LocationService} so the wire error code matches the rule.
 */
public final class LocationDtos {

    private LocationDtos() {
    }

    /** Postal address payload; persisted verbatim as JSONB. */
    public record AddressRequest(
        @NotBlank @Size(min = 1, max = 200) String street,
        @NotBlank @Size(min = 1, max = 120) String city,
        @Size(max = 120) String region,
        @Size(max = 20) String postcode,
        @NotBlank @Size(min = 2, max = 2) String country
    ) {
    }

    /** POST /api/locations body. */
    public record LocationCreateRequest(
        @NotBlank @Size(min = 26, max = 26) String companyId,
        @NotBlank @Size(min = 1, max = 10) String code,
        @NotBlank @Size(min = 1, max = 200) String name,
        @NotBlank @Size(min = 1, max = 32) String type,
        @NotBlank @Size(min = 1, max = 50) String timezone,
        @NotNull @Valid AddressRequest address,
        @Size(max = 30) String phone,
        @Email @Size(max = 200) String publicEmail,
        @Size(max = 26) String defaultPriceListId,
        @Size(max = 26) String defaultTaxCodeId,
        @Size(max = 16) String priceDisplayMode
    ) {
    }

    /**
     * PATCH /api/locations/{id} body — partial update; every field is optional.
     * A null field leaves the stored value untouched. {@code companyId} is
     * intentionally absent (moving a location between companies is unsupported).
     * {@code version} drives the optimistic-lock check.
     */
    public record LocationUpdateRequest(
        @Size(min = 1, max = 10) String code,
        @Size(min = 1, max = 200) String name,
        @Size(min = 1, max = 32) String type,
        @Size(min = 1, max = 50) String timezone,
        @Valid AddressRequest address,
        @Size(max = 30) String phone,
        @Email @Size(max = 200) String publicEmail,
        @Size(max = 26) String defaultPriceListId,
        @Size(max = 26) String defaultTaxCodeId,
        @Size(max = 16) String priceDisplayMode,
        Long version
    ) {
    }

    /** Address as returned to clients. */
    public record AddressResponse(
        String street,
        String city,
        String region,
        String postcode,
        String country
    ) {
    }

    /** Location as returned to clients. {@code id} is the ULID, never the bigint. */
    public record LocationResponse(
        String id,
        String companyId,
        String code,
        String name,
        String type,
        String timezone,
        AddressResponse address,
        String phone,
        String publicEmail,
        String defaultPriceListId,
        String defaultTaxCodeId,
        String status,
        String priceDisplayMode,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {
    }
}

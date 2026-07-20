package com.springboot.erp.modules.crm.customers.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Wire DTOs for ENT-050 Customer + ENT-051 CustomerProfile (records —
 * ARCHITECTURE.md §2). Mirrors the reference Pydantic schemas
 * ({@code CustomerCreateRequest}/{@code CustomerUpdateRequest}/
 * {@code CustomerResponse}/{@code ConsentRecord}).
 */
public final class CustomerDtos {

    private CustomerDtos() {
    }

    /** POST /api/crm/customers body. */
    public record CustomerCreateRequest(
        @NotNull @Size(min = 26, max = 26) String companyId,
        @NotBlank @Size(min = 1, max = 100) String firstName,
        @NotBlank @Size(min = 1, max = 100) String lastName,
        @Size(max = 32) String mobile,
        @Size(max = 255) @Email String email,
        @Size(max = 16) String postcode,
        LocalDate dateOfBirth,
        @Size(min = 26, max = 26) String preferredLocationId,
        String type,
        boolean emailConsent,
        boolean smsConsent,
        boolean analyticsConsent,
        String photoUrl
    ) {
    }

    /** PATCH /api/crm/customers/{id} body — partial update. */
    public record CustomerUpdateRequest(
        @Size(min = 1, max = 100) String firstName,
        @Size(min = 1, max = 100) String lastName,
        @Size(max = 32) String mobile,
        @Size(max = 255) @Email String email,
        @Size(max = 16) String postcode,
        LocalDate dateOfBirth,
        @Size(min = 26, max = 26) String preferredLocationId,
        Boolean analyticsConsent,
        String photoUrl,
        Long version
    ) {
    }

    /** POST /api/crm/customers/{id}/consent body — FR-201 single-channel toggle. */
    public record ConsentToggleRequest(
        @NotBlank String channel,
        @NotNull Boolean granted
    ) {
    }

    /** One channel's current consent state + who/when it was last recorded. */
    public record ConsentRecord(
        String channel,
        boolean granted,
        Instant recordedAt,
        String recordedBy
    ) {
    }

    /** One append-only consent-log row (FR-201 audit trail). */
    public record ConsentLogEntry(
        String id,
        String channel,
        boolean granted,
        Instant recordedAt,
        String recordedBy
    ) {
    }

    /** Customer read shape. {@code id} is the ULID public id. */
    public record CustomerResponse(
        String id,
        String companyId,
        String membershipId,
        String firstName,
        String lastName,
        String type,
        String status,
        String mobile,
        String email,
        String postcode,
        LocalDate dateOfBirth,
        String preferredLocationId,
        boolean analyticsConsent,
        List<ConsentRecord> consents,
        String photoUrl,
        boolean anonymized,
        Instant anonymizedAt,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {
    }
}

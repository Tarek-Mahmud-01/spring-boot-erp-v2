package com.guru.erp.modules.settings.taxcode.dto;

import com.guru.erp.modules.settings.taxcode.domain.GstTreatment;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Tax Code request/response DTOs (ARCHITECTURE.md §2 — records). */
public final class TaxCodeDtos {

    private TaxCodeDtos() {
    }

    /**
     * Create payload (mirrors {@code TaxCodeCreateRequest}). Field constraints
     * mirror the reference Pydantic model; the inverted-window rule
     * ({@code effective_to >= effective_from}) is validated in the service.
     */
    public record TaxCodeCreateRequest(
        @NotBlank @Size(min = 26, max = 26) String companyId,
        @NotBlank @Size(min = 1, max = 20) String code,
        @NotBlank @Size(min = 1, max = 200) String description,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal ratePercent,
        boolean inclusive,
        boolean exempt,
        GstTreatment gstTreatment,
        @NotNull LocalDate effectiveFrom,
        LocalDate effectiveTo
    ) {
    }

    /**
     * Partial update payload (mirrors {@code TaxCodeUpdateRequest}). Null fields
     * are left unchanged. {@code version} drives the optimistic-lock check.
     */
    public record TaxCodeUpdateRequest(
        @Size(min = 1, max = 200) String description,
        @DecimalMin("0") @DecimalMax("100") BigDecimal ratePercent,
        Boolean inclusive,
        Boolean exempt,
        GstTreatment gstTreatment,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        Long version
    ) {
    }

    /** Read shape (mirrors {@code TaxCodeResponse}); {@code id} is the ULID public id. */
    public record TaxCodeResponse(
        String id,
        String companyId,
        String code,
        String description,
        BigDecimal ratePercent,
        boolean inclusive,
        boolean exempt,
        GstTreatment gstTreatment,
        String status,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {
    }
}

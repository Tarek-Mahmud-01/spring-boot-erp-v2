package com.springboot.erp.modules.settings.company.dto;

import com.springboot.erp.modules.settings.company.domain.BasPeriod;
import com.springboot.erp.modules.settings.company.domain.ComplianceProfile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * Wire DTOs for ENT-001 Company (records — ARCHITECTURE.md §2). Bean-validation
 * annotations mirror the reference Pydantic field constraints; cross-field
 * business rules (tax-registered requires a date, AU requires a valid ABN, etc.)
 * live in the service so the wire error code matches the rule.
 */
public final class CompanyDtos {

    private CompanyDtos() {
    }

    /** POST /api/companies body. */
    public record CompanyCreateRequest(
        @NotBlank @Size(min = 3, max = 10) String code,
        @NotBlank @Size(min = 1, max = 200) String legalName,
        @Size(max = 200) String tradingName,
        @NotBlank @Size(min = 2, max = 2) String country,
        @NotBlank @Size(min = 3, max = 3) String baseCurrency,
        @Size(max = 30) String taxRegistrationNo,
        boolean taxRegistered,
        LocalDate taxRegistrationDate,
        @NotBlank @Size(min = 5, max = 5) String fiscalYearStart,
        ComplianceProfile complianceProfile,
        @Size(max = 14) String abn,
        @Size(max = 12) String acn,
        LocalDate gstRegistrationDate,
        BasPeriod basPeriod,
        String logoUrl,
        Map<String, Object> invoiceLayout
    ) {
    }

    /**
     * PATCH /api/companies/{id} body — every field optional, only present fields
     * are applied. {@code baseCurrency} is intentionally absent (immutable after
     * creation). {@code version} carries the client's optimistic-lock token.
     */
    public record CompanyUpdateRequest(
        @Size(min = 1, max = 200) String legalName,
        @Size(max = 200) String tradingName,
        @Size(min = 2, max = 2) String country,
        @Size(max = 30) String taxRegistrationNo,
        Boolean taxRegistered,
        LocalDate taxRegistrationDate,
        @Size(min = 5, max = 5) String fiscalYearStart,
        ComplianceProfile complianceProfile,
        @Size(max = 14) String abn,
        @Size(max = 12) String acn,
        LocalDate gstRegistrationDate,
        BasPeriod basPeriod,
        String logoUrl,
        Map<String, Object> invoiceLayout,
        Long version
    ) {
    }

    /** Company read shape. {@code id} is the ULID public id; {@code status} is the derived string. */
    public record CompanyResponse(
        String id,
        String code,
        String legalName,
        String tradingName,
        String country,
        String baseCurrency,
        String taxRegistrationNo,
        boolean taxRegistered,
        LocalDate taxRegistrationDate,
        String fiscalYearStart,
        boolean primary,
        String status,
        ComplianceProfile complianceProfile,
        String abn,
        String acn,
        LocalDate gstRegistrationDate,
        BasPeriod basPeriod,
        String logoUrl,
        Map<String, Object> invoiceLayout,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {
    }
}

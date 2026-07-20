package com.springboot.erp.modules.reporting.posloyalty.dto;

import com.springboot.erp.platform.web.PageResponse;
import java.time.Instant;
import java.util.List;

/**
 * Wire DTOs for the CRM &amp; Loyalty half of the REPORTING "pos-loyalty" sub-slice (reference
 * {@code app.reports.schemas.loyalty}: RPT-022 Loyalty Ledger, RPT-021 Customer List w/ Consent).
 * Records only (ARCHITECTURE.md §2).
 */
public final class LoyaltyReportDtos {

    private LoyaltyReportDtos() {
    }

    // -----------------------------------------------------------------
    // RPT-022 — Loyalty Ledger (per-customer point movements + running balance)
    // -----------------------------------------------------------------

    public record LoyaltyLedgerRow(
        String id,
        Instant occurredAt,
        String membershipId,
        String customerName,
        String kind,
        long points,
        long runningBalance,
        String referenceId,
        String description
    ) {
    }

    public record LoyaltyLedgerSummary(
        String companyId,
        long movements,
        long pointsEarned,
        long pointsSpent,
        long netChange
    ) {
    }

    public record LoyaltyLedgerResponse(PageResponse<LoyaltyLedgerRow> page, LoyaltyLedgerSummary summary) {
    }

    // -----------------------------------------------------------------
    // RPT-021 — Customer List with Consent (consent-aware marketing extract)
    // -----------------------------------------------------------------

    public record ConsentRow(
        String customerId,
        String membershipId,
        String name,
        String email,
        String mobile,
        String postcode,
        List<String> segmentCodes,
        boolean emailConsent,
        boolean smsConsent,
        boolean analyticsConsent,
        Instant lastConsentAt,
        long rolling12mSpendMinor,
        String rolling12mSpendCurrency
    ) {
    }

    public record CustomersWithConsentSummary(
        String companyId,
        long customers,
        long emailOptin,
        long smsOptin,
        long analyticsOptin
    ) {
    }

    public record CustomersWithConsentResponse(
        PageResponse<ConsentRow> page,
        CustomersWithConsentSummary summary
    ) {
    }
}

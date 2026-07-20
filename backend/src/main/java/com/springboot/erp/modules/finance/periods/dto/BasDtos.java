package com.springboot.erp.modules.finance.periods.dto;

import com.springboot.erp.modules.finance.periods.domain.BasPeriodStatus;
import com.springboot.erp.modules.finance.periods.domain.BasPeriodType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/** Wire DTOs for BAS (Business Activity Statement) periods + reports (reference US-AU-005). */
public final class BasDtos {

    private BasDtos() {
    }

    /** POST /api/finance/bas-periods body. */
    public record BasPeriodCreateRequest(
        @NotNull @Size(min = 26, max = 26) String companyId,
        @NotNull @Size(min = 1, max = 20) String periodCode,
        @NotNull BasPeriodType periodType,
        @NotNull LocalDate dateFrom,
        @NotNull LocalDate dateTo,
        /** The company's GL account used for GST/GST-payable postings — drives the 1A/1B split. */
        @NotNull @Size(min = 26, max = 26) String gstAccountId,
        /** The company's GL revenue account — drives G1 (total sales). */
        @NotNull @Size(min = 26, max = 26) String revenueAccountId
    ) {
    }

    /** POST /api/finance/bas-periods/{id}/transition body. */
    public record BasPeriodTransitionRequest(
        @NotNull BasPeriodStatus newStatus,
        @Size(max = 120) String lodgementReference
    ) {
    }

    public record BasPeriodResponse(
        String id,
        String companyId,
        String periodCode,
        BasPeriodType periodType,
        LocalDate dateFrom,
        LocalDate dateTo,
        BasPeriodStatus status,
        Instant lodgedAt,
        String lodgementReference,
        long version
    ) {
    }

    /**
     * All 7 BAS boxes + net GST (reference {@code BasReportResponse}). This slice computes every
     * box from POSTED GL journal lines only (own the GL account explicitly, via
     * {@code gstAccountId}/{@code revenueAccountId} on the {@code BasPeriod} — the reference's
     * fuller {@code bas_report()} additionally sources G2/G3/G10/G11 from raw POS transaction lines
     * and supplier bill lines, which belong to the pos/procurement modules, not this finance/periods
     * slice; those boxes are therefore always 0 here — a documented scope decision, not a bug. 1A/1B
     * and G1 (the GL-derivable boxes) are fully computed, split by voucher type exactly like the
     * reference {@code BasReportView.summary}.
     */
    public record BasReportSnapshotResponse(
        String id,
        String basPeriodId,
        Instant generatedAt,
        int versionNo,
        Map<String, Long> boxValues
    ) {
    }
}

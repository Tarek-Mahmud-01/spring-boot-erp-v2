package com.guru.erp.modules.finance.periods.dto;

import com.guru.erp.modules.finance.periods.domain.FiscalPeriodStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Wire DTOs for the "fiscal periods" concept (reference {@code FiscalPeriodResponse} /
 * {@code FiscalPeriodCreateRequest} / {@code FiscalPeriodTransitionRequest} in
 * {@code app.finance.schemas}).
 */
public final class FiscalPeriodDtos {

    private FiscalPeriodDtos() {
    }

    /** POST /api/finance/fiscal-periods body — manual creation with a custom date range. */
    public record FiscalPeriodCreateRequest(
        @NotNull @Size(min = 26, max = 26) String companyId,
        @NotNull @Size(min = 1, max = 7) String periodCode,
        @NotNull LocalDate dateFrom,
        @NotNull LocalDate dateTo
    ) {
    }

    /** POST /api/finance/fiscal-periods/{id}/transition body. */
    public record FiscalPeriodTransitionRequest(
        @NotNull FiscalPeriodStatus newStatus,
        @Size(max = 500) String reason
    ) {
    }

    public record FiscalPeriodResponse(
        String id,
        String companyId,
        String periodCode,
        LocalDate dateFrom,
        LocalDate dateTo,
        FiscalPeriodStatus status,
        long version
    ) {
    }
}

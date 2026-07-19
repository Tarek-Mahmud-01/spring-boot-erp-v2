package com.guru.erp.modules.finance.periods.dto;

import com.guru.erp.modules.finance.periods.domain.ChecklistCheckStatus;
import com.guru.erp.modules.finance.periods.domain.ChecklistItemKey;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;

/** Wire DTOs for the period-close checklist (reference FR-249, {@code app.finance.schemas}). */
public final class PeriodChecklistDtos {

    private PeriodChecklistDtos() {
    }

    /** POST /api/finance/fiscal-periods/{id}/checklist/{itemKey}/owner body. */
    public record PeriodChecklistOwnerRequest(
        @Size(min = 26, max = 26) String ownerUserId
    ) {
    }

    public record PeriodChecklistItemResponse(
        String id,
        ChecklistItemKey itemKey,
        boolean required,
        String ownerUserId,
        ChecklistCheckStatus checkStatus,
        Map<String, Object> checkDetail,
        Instant checkedAt,
        String signedOffBy,
        Instant signedOffAt,
        long version
    ) {
    }
}

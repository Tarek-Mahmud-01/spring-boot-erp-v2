package com.guru.erp.modules.finance.periods.dto;

import com.guru.erp.modules.finance.periods.domain.ApprovalStepStatus;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Wire DTOs for the period-close approval chain (E-009 Phase 4, {@code app.finance.schemas}). */
public final class PeriodApprovalDtos {

    private PeriodApprovalDtos() {
    }

    /** POST /api/finance/fiscal-periods/{id}/approvals/{sequence}/approve|reject body. */
    public record PeriodApprovalDecisionRequest(
        @Size(max = 500) String comment
    ) {
    }

    public record PeriodApprovalStepResponse(
        String id,
        int sequence,
        String label,
        ApprovalStepStatus status,
        String approverUserId,
        Instant decidedAt,
        String comment,
        long version
    ) {
    }
}

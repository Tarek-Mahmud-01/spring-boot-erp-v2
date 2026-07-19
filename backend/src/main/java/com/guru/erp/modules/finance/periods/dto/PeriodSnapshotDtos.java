package com.guru.erp.modules.finance.periods.dto;

import java.time.Instant;
import java.util.Map;

/** Wire DTOs for the frozen period-close snapshot (E-009 Phase 5, read-only historical record). */
public final class PeriodSnapshotDtos {

    private PeriodSnapshotDtos() {
    }

    public record PeriodSnapshotResponse(
        String id,
        String fiscalPeriodId,
        Instant generatedAt,
        String generatedBy,
        int versionNo,
        Map<String, Object> payload
    ) {
    }
}

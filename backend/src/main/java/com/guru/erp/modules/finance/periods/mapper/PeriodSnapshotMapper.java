package com.guru.erp.modules.finance.periods.mapper;

import com.guru.erp.modules.finance.periods.domain.PeriodSnapshot;
import com.guru.erp.modules.finance.periods.dto.PeriodSnapshotDtos.PeriodSnapshotResponse;
import org.springframework.stereotype.Component;

@Component
public class PeriodSnapshotMapper {

    public PeriodSnapshotResponse toResponse(PeriodSnapshot snap) {
        return new PeriodSnapshotResponse(
            snap.getPublicId(),
            snap.getFiscalPeriod().getPublicId(),
            snap.getGeneratedAt(),
            snap.getGeneratedBy(),
            snap.getVersionNo(),
            snap.getPayload());
    }
}

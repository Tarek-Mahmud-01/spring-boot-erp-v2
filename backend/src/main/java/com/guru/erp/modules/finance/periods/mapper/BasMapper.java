package com.guru.erp.modules.finance.periods.mapper;

import com.guru.erp.modules.finance.periods.domain.BasPeriod;
import com.guru.erp.modules.finance.periods.domain.BasReport;
import com.guru.erp.modules.finance.periods.dto.BasDtos.BasPeriodResponse;
import com.guru.erp.modules.finance.periods.dto.BasDtos.BasReportSnapshotResponse;
import org.springframework.stereotype.Component;

@Component
public class BasMapper {

    public BasPeriodResponse toResponse(BasPeriod p) {
        return new BasPeriodResponse(
            p.getPublicId(),
            p.getCompanyId(),
            p.getPeriodCode(),
            p.getPeriodType(),
            p.getDateFrom(),
            p.getDateTo(),
            p.getStatus(),
            p.getLodgedAt(),
            p.getLodgementReference(),
            p.getVersion());
    }

    public BasReportSnapshotResponse toResponse(BasReport r) {
        return new BasReportSnapshotResponse(
            r.getPublicId(),
            r.getBasPeriod().getPublicId(),
            r.getGeneratedAt(),
            r.getVersionNo(),
            r.getBoxValues());
    }
}

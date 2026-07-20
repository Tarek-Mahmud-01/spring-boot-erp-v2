package com.springboot.erp.modules.finance.periods.mapper;

import com.springboot.erp.modules.finance.periods.domain.FiscalPeriod;
import com.springboot.erp.modules.finance.periods.dto.FiscalPeriodDtos.FiscalPeriodResponse;
import org.springframework.stereotype.Component;

@Component
public class FiscalPeriodMapper {

    public FiscalPeriodResponse toResponse(FiscalPeriod p) {
        return new FiscalPeriodResponse(
            p.getPublicId(),
            p.getCompanyId(),
            p.getPeriodCode(),
            p.getDateFrom(),
            p.getDateTo(),
            p.getStatus(),
            p.getVersion());
    }
}

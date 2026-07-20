package com.springboot.erp.modules.finance.periods.mapper;

import com.springboot.erp.modules.finance.periods.domain.PeriodChecklistItem;
import com.springboot.erp.modules.finance.periods.dto.PeriodChecklistDtos.PeriodChecklistItemResponse;
import org.springframework.stereotype.Component;

@Component
public class PeriodChecklistMapper {

    public PeriodChecklistItemResponse toResponse(PeriodChecklistItem item) {
        return new PeriodChecklistItemResponse(
            item.getPublicId(),
            item.getItemKey(),
            item.isRequired(),
            item.getOwnerUserId(),
            item.getCheckStatus(),
            item.getCheckDetail(),
            item.getCheckedAt(),
            item.getSignedOffBy(),
            item.getSignedOffAt(),
            item.getVersion());
    }
}

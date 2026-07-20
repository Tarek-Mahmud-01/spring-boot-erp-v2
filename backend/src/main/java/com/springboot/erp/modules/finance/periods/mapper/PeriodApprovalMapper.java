package com.springboot.erp.modules.finance.periods.mapper;

import com.springboot.erp.modules.finance.periods.domain.PeriodApprovalStep;
import com.springboot.erp.modules.finance.periods.dto.PeriodApprovalDtos.PeriodApprovalStepResponse;
import org.springframework.stereotype.Component;

@Component
public class PeriodApprovalMapper {

    public PeriodApprovalStepResponse toResponse(PeriodApprovalStep step) {
        return new PeriodApprovalStepResponse(
            step.getPublicId(),
            step.getSequence(),
            step.getLabel(),
            step.getStatus(),
            step.getApproverUserId(),
            step.getDecidedAt(),
            step.getComment(),
            step.getVersion());
    }
}

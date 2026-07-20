package com.springboot.erp.modules.finance.periods.repository;

import com.springboot.erp.modules.finance.periods.domain.PeriodApprovalStep;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeriodApprovalStepRepository extends JpaRepository<PeriodApprovalStep, Long> {

    List<PeriodApprovalStep> findByFiscalPeriodIdOrderBySequenceAsc(Long fiscalPeriodId);

    Optional<PeriodApprovalStep> findByFiscalPeriodIdAndSequence(Long fiscalPeriodId, int sequence);

    Optional<PeriodApprovalStep> findByPublicId(String publicId);
}

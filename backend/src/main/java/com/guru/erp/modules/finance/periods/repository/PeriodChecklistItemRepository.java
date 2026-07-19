package com.guru.erp.modules.finance.periods.repository;

import com.guru.erp.modules.finance.periods.domain.ChecklistItemKey;
import com.guru.erp.modules.finance.periods.domain.PeriodChecklistItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeriodChecklistItemRepository extends JpaRepository<PeriodChecklistItem, Long> {

    List<PeriodChecklistItem> findByFiscalPeriodIdOrderByIdAsc(Long fiscalPeriodId);

    Optional<PeriodChecklistItem> findByFiscalPeriodIdAndItemKey(Long fiscalPeriodId, ChecklistItemKey itemKey);

    Optional<PeriodChecklistItem> findByPublicId(String publicId);
}

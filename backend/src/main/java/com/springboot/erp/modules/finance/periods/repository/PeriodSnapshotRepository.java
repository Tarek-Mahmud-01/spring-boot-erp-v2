package com.springboot.erp.modules.finance.periods.repository;

import com.springboot.erp.modules.finance.periods.domain.PeriodSnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeriodSnapshotRepository extends JpaRepository<PeriodSnapshot, Long> {

    List<PeriodSnapshot> findByFiscalPeriodIdOrderByVersionNoDesc(Long fiscalPeriodId);

    Optional<PeriodSnapshot> findTopByFiscalPeriodIdOrderByVersionNoDesc(Long fiscalPeriodId);
}

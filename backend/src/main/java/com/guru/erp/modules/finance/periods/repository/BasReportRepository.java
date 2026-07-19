package com.guru.erp.modules.finance.periods.repository;

import com.guru.erp.modules.finance.periods.domain.BasReport;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BasReportRepository extends JpaRepository<BasReport, Long> {

    List<BasReport> findByBasPeriodIdOrderByVersionNoDesc(Long basPeriodId);

    Optional<BasReport> findTopByBasPeriodIdOrderByVersionNoDesc(Long basPeriodId);
}

package com.springboot.erp.modules.finance.periods.repository;

import com.springboot.erp.modules.finance.periods.domain.BasPeriod;
import com.springboot.erp.modules.finance.periods.domain.BasPeriodStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BasPeriodRepository extends JpaRepository<BasPeriod, Long> {

    Optional<BasPeriod> findByPublicId(String publicId);

    boolean existsByCompanyIdAndPeriodCode(String companyId, String periodCode);

    @Query("""
        select p from BasPeriod p
        where p.companyId = :companyId
          and (:status is null or p.status = :status)
        order by p.dateFrom desc
        """)
    Page<BasPeriod> search(@Param("companyId") String companyId,
                           @Param("status") BasPeriodStatus status,
                           Pageable pageable);
}

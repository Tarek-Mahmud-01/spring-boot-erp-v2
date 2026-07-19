package com.guru.erp.modules.finance.periods.repository;

import com.guru.erp.modules.finance.periods.domain.FiscalPeriod;
import com.guru.erp.modules.finance.periods.domain.FiscalPeriodStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FiscalPeriodRepository extends JpaRepository<FiscalPeriod, Long> {

    Optional<FiscalPeriod> findByPublicId(String publicId);

    boolean existsByCompanyIdAndPeriodCode(String companyId, String periodCode);

    /** AC-043-3/AC-048 posting-gate resolution: the period whose date range contains {@code onDate}. */
    @Query("""
        select p from FiscalPeriod p
        where p.companyId = :companyId and p.dateFrom <= :onDate and p.dateTo >= :onDate
        """)
    Optional<FiscalPeriod> findContaining(@Param("companyId") String companyId, @Param("onDate") LocalDate onDate);

    @Query("""
        select p from FiscalPeriod p
        where p.companyId = :companyId
          and (:q is null or lower(p.periodCode) like lower(concat('%', :q, '%')))
          and (:status is null or p.status = :status)
          and (:dateFrom is null or p.dateFrom >= :dateFrom)
          and (:dateTo is null or p.dateTo <= :dateTo)
        order by p.dateFrom
        """)
    Page<FiscalPeriod> search(@Param("companyId") String companyId,
                              @Param("q") String q,
                              @Param("status") FiscalPeriodStatus status,
                              @Param("dateFrom") LocalDate dateFrom,
                              @Param("dateTo") LocalDate dateTo,
                              Pageable pageable);

    @Query("""
        select p from FiscalPeriod p
        where p.companyId = :companyId
          and p.dateFrom <= :dateTo and p.dateTo >= :dateFrom
        """)
    List<FiscalPeriod> findOverlapping(@Param("companyId") String companyId,
                                       @Param("dateFrom") LocalDate dateFrom,
                                       @Param("dateTo") LocalDate dateTo);
}

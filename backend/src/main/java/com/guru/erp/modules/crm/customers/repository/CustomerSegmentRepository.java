package com.guru.erp.modules.crm.customers.repository;

import com.guru.erp.modules.crm.customers.domain.CustomerSegment;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link CustomerSegment}. Soft-deleted rows are excluded
 * automatically by the {@code @SQLRestriction} on {@code BaseEntity}.
 */
public interface CustomerSegmentRepository extends JpaRepository<CustomerSegment, Long> {

    Optional<CustomerSegment> findByPublicId(String publicId);

    boolean existsByCompanyIdAndCode(String companyId, String code);

    boolean existsByCompanyIdAndCodeAndPublicIdNot(String companyId, String code, String publicId);

    @Query("""
        select s from CustomerSegment s
        where (:companyId is null or s.companyId = :companyId)
        order by s.createdAt desc
        """)
    Page<CustomerSegment> search(@Param("companyId") String companyId, Pageable pageable);
}

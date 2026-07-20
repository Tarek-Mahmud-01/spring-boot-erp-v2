package com.springboot.erp.modules.product.promotions.repository;

import com.springboot.erp.modules.product.promotions.domain.ReasonCode;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link ReasonCode}. Soft-deleted rows are excluded by the
 * {@code @SQLRestriction} on {@link com.springboot.erp.platform.entity.BaseEntity}, so
 * the case-insensitive uniqueness probe only sees live codes — a soft-deleted
 * code may be re-introduced later (FR-068).
 */
public interface ReasonCodeRepository extends JpaRepository<ReasonCode, Long> {

    Optional<ReasonCode> findByPublicId(String publicId);

    /** Case-insensitive code-uniqueness probe among live rows (excludes one id). */
    @Query("""
        select (count(r) > 0) from ReasonCode r
        where lower(r.code) = lower(:code)
          and (:excludeId is null or r.id <> :excludeId)
        """)
    boolean existsByCode(@Param("code") String code, @Param("excludeId") Long excludeId);

    /**
     * Non-deleted reason codes ordered by code, optionally filtered to active
     * only (mirrors the reference {@code list_reason_codes(active_only=...)}).
     */
    @Query("""
        select r from ReasonCode r
        where (:activeOnly = false or r.isActive = true)
        order by r.code
        """)
    Page<ReasonCode> search(@Param("activeOnly") boolean activeOnly, Pageable pageable);
}

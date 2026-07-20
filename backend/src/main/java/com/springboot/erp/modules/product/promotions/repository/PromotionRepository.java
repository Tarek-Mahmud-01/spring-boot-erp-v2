package com.springboot.erp.modules.product.promotions.repository;

import com.springboot.erp.modules.product.promotions.domain.Promotion;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link Promotion}. The {@code @SQLRestriction} on
 * {@link com.springboot.erp.platform.entity.BaseEntity} already excludes soft-deleted
 * rows from every query below.
 */
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    Optional<Promotion> findByPublicId(String publicId);

    /**
     * Non-deleted promotions, optionally filtered by owning company ULID,
     * ordered newest-window-first (mirrors the reference {@code list_promotions}).
     */
    @Query("""
        select p from Promotion p
        where (:companyId is null or p.companyId = :companyId)
        order by p.dateFrom desc
        """)
    Page<Promotion> search(@Param("companyId") String companyId, Pageable pageable);

    /** Case-insensitive name-uniqueness probe within a company (excludes one id). */
    @Query("""
        select (count(p) > 0) from Promotion p
        where p.companyId = :companyId
          and lower(p.name) = lower(:name)
          and (:excludeId is null or p.id <> :excludeId)
        """)
    boolean existsByCompanyAndName(@Param("companyId") String companyId,
                                   @Param("name") String name,
                                   @Param("excludeId") Long excludeId);
}

package com.guru.erp.modules.product.pricing.repository;

import com.guru.erp.modules.product.pricing.domain.PriceList;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link PriceList}. The {@code @SQLRestriction} on
 * {@link com.guru.erp.platform.entity.BaseEntity} already excludes soft-deleted
 * rows from every query below.
 */
public interface PriceListRepository extends JpaRepository<PriceList, Long> {

    Optional<PriceList> findByPublicId(String publicId);

    /**
     * Case-insensitive duplicate-name guard scoped to one company (FR-061 —
     * uniqueness on the trimmed, case-folded name). {@code excludeId} skips the
     * row being updated (pass a value that never matches, e.g. -1, on create).
     */
    @Query("""
        select (count(pl) > 0) from PriceList pl
        where pl.companyId = :companyId
          and lower(pl.name) = lower(:name)
          and pl.id <> :excludeId
        """)
    boolean existsByCompanyIdAndNameIgnoreCase(@Param("companyId") String companyId,
                                               @Param("name") String name,
                                               @Param("excludeId") long excludeId);

    /** Non-deleted price lists, optionally filtered by company publicId. */
    @Query("""
        select pl from PriceList pl
        where (:companyId is null or pl.companyId = :companyId)
        order by pl.name
        """)
    Page<PriceList> search(@Param("companyId") String companyId, Pageable pageable);
}

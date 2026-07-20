package com.springboot.erp.modules.inventory.counts.repository;

import com.springboot.erp.modules.inventory.counts.domain.StockOpening;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence for {@link StockOpening}. Soft-deleted rows are excluded
 * automatically by the {@code @SQLRestriction} on {@code BaseEntity}.
 */
public interface StockOpeningRepository extends JpaRepository<StockOpening, Long> {

    Optional<StockOpening> findByPublicId(String publicId);

    Page<StockOpening> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<StockOpening> findByLocationIdOrderByCreatedAtDesc(String locationId, Pageable pageable);

    /**
     * The one-live-opening guard: is there any non-deleted opening (Draft OR
     * Posted) for this (company, product, variant, location)? {@code variantId}
     * is matched as-is including null. Mirrors the reference partial unique index.
     */
    @Query("""
        select count(o) > 0 from StockOpening o
        where o.companyId = :companyId
          and o.productId = :productId
          and o.locationId = :locationId
          and ((:variantId is null and o.variantId is null) or o.variantId = :variantId)
        """)
    boolean existsLiveOpening(@Param("companyId") String companyId,
                              @Param("productId") String productId,
                              @Param("variantId") String variantId,
                              @Param("locationId") String locationId);
}

package com.springboot.erp.modules.pos.auxiliary.repository;

import com.springboot.erp.modules.pos.auxiliary.domain.PosParkedSale;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link PosParkedSale}. Soft-deleted rows are excluded automatically by the
 * {@code @SQLRestriction} on {@code BaseEntity}.
 */
public interface PosParkedSaleRepository extends JpaRepository<PosParkedSale, Long> {

    Optional<PosParkedSale> findByPublicId(String publicId);

    /** US-035 FR-184/185 — the active (not yet resumed) park under this code, if any. */
    Optional<PosParkedSale> findByParkCodeAndResumedAtIsNull(String parkCode);

    boolean existsByParkCodeAndResumedAtIsNull(String parkCode);

    /** US-035 FR-184 — list active parks, optionally scoped to one location. */
    @Query("""
        select p from PosParkedSale p
        where p.resumedAt is null
          and (:locationId is null or p.locationId = :locationId)
        order by p.parkedAt desc
        """)
    Page<PosParkedSale> findActive(@Param("locationId") String locationId, Pageable pageable);
}

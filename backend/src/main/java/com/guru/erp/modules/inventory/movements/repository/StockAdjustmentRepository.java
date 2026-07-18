package com.guru.erp.modules.inventory.movements.repository;

import com.guru.erp.modules.inventory.movements.domain.AdjustmentStatus;
import com.guru.erp.modules.inventory.movements.domain.StockAdjustment;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link StockAdjustment}. Soft-deleted rows are excluded automatically by the
 * {@code @SQLRestriction} on {@code BaseEntity}.
 */
public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {

    Optional<StockAdjustment> findByPublicId(String publicId);

    boolean existsByNumber(String number);

    /** List with optional location, status, and free-text (number OR reason) filters. */
    @Query("""
        select a from StockAdjustment a
        where (:locationId is null or a.locationId = :locationId)
          and (:status is null or a.status = :status)
          and (:search is null
               or lower(a.number) like lower(concat('%', :search, '%'))
               or lower(a.reason) like lower(concat('%', :search, '%')))
        order by a.createdAt desc
        """)
    Page<StockAdjustment> search(@Param("locationId") String locationId,
                                 @Param("status") AdjustmentStatus status,
                                 @Param("search") String search,
                                 Pageable pageable);
}

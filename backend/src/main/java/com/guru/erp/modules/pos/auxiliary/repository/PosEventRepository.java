package com.guru.erp.modules.pos.auxiliary.repository;

import com.guru.erp.modules.pos.auxiliary.domain.PosEvent;
import com.guru.erp.modules.pos.auxiliary.domain.PosEventType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link PosEvent}. Soft-deleted rows are excluded automatically by the
 * {@code @SQLRestriction} on {@code BaseEntity}. Append-only from the domain's perspective — rows
 * are never deleted, only marked reviewed.
 */
public interface PosEventRepository extends JpaRepository<PosEvent, Long> {

    Optional<PosEvent> findByPublicId(String publicId);

    /** Filtered timeline: optional type, register, needs-review, and transaction scoping. */
    @Query("""
        select e from PosEvent e
        where (:type is null or e.type = :type)
          and (:registerId is null or e.registerId = :registerId)
          and (:transactionId is null or e.transactionId = :transactionId)
          and (:needsReview is null or e.needsReview = :needsReview)
        order by e.createdAt desc
        """)
    Page<PosEvent> search(@Param("type") PosEventType type,
                          @Param("registerId") String registerId,
                          @Param("transactionId") String transactionId,
                          @Param("needsReview") Boolean needsReview,
                          Pageable pageable);
}

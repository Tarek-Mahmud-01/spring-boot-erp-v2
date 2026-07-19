package com.guru.erp.modules.pos.auxiliary.repository;

import com.guru.erp.modules.pos.auxiliary.domain.PosRefund;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link PosRefund}. Soft-deleted rows are excluded automatically by the
 * {@code @SQLRestriction} on {@code BaseEntity}.
 */
public interface PosRefundRepository extends JpaRepository<PosRefund, Long> {

    Optional<PosRefund> findByPublicId(String publicId);

    /** Idempotency / lookup — the refund metadata row for a given REFUND transaction. */
    Optional<PosRefund> findByTransactionId(String transactionId);

    /** List refunds, optionally scoped to the original (refunded) transaction. */
    @Query("""
        select r from PosRefund r
        where (:originalTransactionId is null or r.originalTransactionId = :originalTransactionId)
        order by r.createdAt desc
        """)
    Page<PosRefund> search(@Param("originalTransactionId") String originalTransactionId, Pageable pageable);
}

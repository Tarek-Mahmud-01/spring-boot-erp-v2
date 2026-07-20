package com.springboot.erp.modules.pos.transactions.repository;

import com.springboot.erp.modules.pos.transactions.domain.PosTransaction;
import com.springboot.erp.modules.pos.transactions.domain.PosTransactionStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link PosTransaction}. Soft-deleted rows are excluded automatically by the
 * {@code @SQLRestriction} on {@code BaseEntity}.
 */
public interface PosTransactionRepository extends JpaRepository<PosTransaction, Long> {

    Optional<PosTransaction> findByPublicId(String publicId);

    Optional<PosTransaction> findByClientTxnUuid(String clientTxnUuid);

    long countByReceiptNumberStartingWith(String prefix);

    /** List with optional register, location, status, and free-text (receipt number) filters. */
    @Query("""
        select t from PosTransaction t
        where (:registerId is null or t.registerId = :registerId)
          and (:locationId is null or t.locationId = :locationId)
          and (:status is null or t.status = :status)
          and (:search is null or lower(t.receiptNumber) like lower(concat('%', cast(:search as string), '%')))
        order by t.createdAt desc
        """)
    Page<PosTransaction> search(@Param("registerId") String registerId,
                                @Param("locationId") String locationId,
                                @Param("status") PosTransactionStatus status,
                                @Param("search") String search,
                                Pageable pageable);
}

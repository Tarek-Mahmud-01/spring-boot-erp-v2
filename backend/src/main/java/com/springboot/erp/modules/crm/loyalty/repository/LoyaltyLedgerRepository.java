package com.springboot.erp.modules.crm.loyalty.repository;

import com.springboot.erp.modules.crm.loyalty.domain.LoyaltyLedger;
import com.springboot.erp.modules.crm.loyalty.domain.LoyaltyMovementType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence for the append-only {@link LoyaltyLedger} (the closest analog to
 * {@code StockLedgerRepository}). Writers only insert; corrections are new
 * rows, plus in-place updates of the FIFO {@code remaining} bookkeeping field
 * on EARN rows performed by the owning service. {@code @SQLRestriction} on
 * {@code BaseEntity} excludes soft-deleted rows automatically (not used in
 * practice here, since ledger rows are never deleted).
 */
public interface LoyaltyLedgerRepository extends JpaRepository<LoyaltyLedger, Long> {

    Optional<LoyaltyLedger> findByPublicId(String publicId);

    /** Paginated ledger listing for one customer, newest-first (reference {@code _ledger_entries}). */
    Page<LoyaltyLedger> findByCustomerIdOrderByOccurredAtDescIdDesc(String customerId, Pageable pageable);

    /**
     * FIFO-consumable EARN lots with points remaining, oldest-expiry-first,
     * expiry-less lots last (reference {@code _consume_lots}). Cannot use
     * {@code nulls last} portably in JPQL, so the null/non-null buckets are
     * ordered via a boolean-cast expression instead.
     */
    @Query("""
        select l from LoyaltyLedger l
        where l.customerId = :customerId
          and l.type = com.springboot.erp.modules.crm.loyalty.domain.LoyaltyMovementType.EARN
          and l.remaining > 0
          and (:sourceTransactionId is null or l.sourceTransactionId = :sourceTransactionId)
        order by case when l.expiresAt is null then 1 else 0 end, l.expiresAt asc, l.id asc
        """)
    List<LoyaltyLedger> findEarnLotsWithRemaining(
        @Param("customerId") String customerId,
        @Param("sourceTransactionId") String sourceTransactionId);

    /** Earliest unexpired EARN lot with points remaining — the FIFO head (reference {@code _balance_response}). */
    @Query("""
        select l from LoyaltyLedger l
        where l.customerId = :customerId
          and l.type = com.springboot.erp.modules.crm.loyalty.domain.LoyaltyMovementType.EARN
          and l.remaining > 0
          and l.expiresAt is not null
        order by l.expiresAt asc
        """)
    List<LoyaltyLedger> findNextExpiringLots(@Param("customerId") String customerId);

    /** EARN lots past expiry as of {@code asOf}, for the expiry sweep (reference {@code expire_points}). */
    @Query("""
        select l from LoyaltyLedger l
        where l.customerId = :customerId
          and l.type = com.springboot.erp.modules.crm.loyalty.domain.LoyaltyMovementType.EARN
          and l.remaining > 0
          and l.expiresAt is not null
          and l.expiresAt <= :asOf
        order by l.expiresAt asc
        """)
    List<LoyaltyLedger> findExpiredLots(@Param("customerId") String customerId, @Param("asOf") Instant asOf);

    List<LoyaltyLedger> findByCustomerIdAndTypeAndSourceTransactionId(
        String customerId, LoyaltyMovementType type, String sourceTransactionId);
}

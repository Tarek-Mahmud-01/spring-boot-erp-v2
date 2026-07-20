package com.springboot.erp.modules.inventory.stock.repository;

import com.springboot.erp.modules.inventory.stock.domain.StockLedger;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence for the append-only {@link StockLedger}. The ledger is never
 * updated in place — writers only insert. Reads are paginated/filtered listings
 * and the aggregate on-hand / availability projections that back the read-only
 * stock views (reference {@code list_ledger_entries}, {@code get_stock_on_hand},
 * {@code stock_availability}). {@code @SQLRestriction} on {@code BaseEntity}
 * excludes soft-deleted rows from every query automatically.
 */
public interface StockLedgerRepository extends JpaRepository<StockLedger, Long> {

    Optional<StockLedger> findByPublicId(String publicId);

    /**
     * Filtered, date-windowed ledger listing ordered newest-first (reference
     * {@code list_ledger_entries}). Every filter is optional (null = ignore);
     * the {@code fromDate} lower bound is always supplied by the service so a
     * huge ledger never sorts unbounded.
     */
    @Query("""
        select l from StockLedger l
        where (:productId is null or l.productId = :productId)
          and (:variantId is null or l.variantId = :variantId)
          and (:locationId is null or l.locationId = :locationId)
          and (:movementType is null or l.movementType = :movementType)
          and l.occurredAt >= :fromDate
          and (:toDate is null or l.occurredAt <= :toDate)
        order by l.occurredAt desc
        """)
    List<StockLedger> listEntries(
        @Param("productId") String productId,
        @Param("variantId") String variantId,
        @Param("locationId") String locationId,
        @Param("movementType") com.springboot.erp.modules.inventory.stock.domain.MovementType movementType,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate,
        Pageable pageable);

    /**
     * On-hand aggregate: {@code Σ qty_signed} grouped by (product, variant,
     * location, status), keeping only non-zero buckets, with the latest movement
     * timestamp (reference {@code get_stock_on_hand}). Filters are optional.
     */
    @Query("""
        select l.productId as productId, l.variantId as variantId,
               l.locationId as locationId, l.status as status,
               sum(l.qtySigned) as qtyOnHand, max(l.occurredAt) as updatedAt
        from StockLedger l
        where (:locationId is null or l.locationId = :locationId)
          and (:productId is null or l.productId = :productId)
          and (:variantId is null or l.variantId = :variantId)
        group by l.productId, l.variantId, l.locationId, l.status
        having sum(l.qtySigned) <> 0
        """)
    List<StockOnHandRow> aggregateOnHand(
        @Param("locationId") String locationId,
        @Param("productId") String productId,
        @Param("variantId") String variantId);

    /**
     * Availability aggregate for a bulk stock-check: {@code Σ qty_signed} grouped
     * by (product, variant, location) restricted to one status, for many products
     * and locations in a single query (reference {@code stock_availability}).
     */
    @Query("""
        select l.productId as productId, l.variantId as variantId,
               l.locationId as locationId, coalesce(sum(l.qtySigned), 0) as onHand
        from StockLedger l
        where l.productId in :productIds
          and l.locationId in :locationIds
          and l.status = :status
        group by l.productId, l.variantId, l.locationId
        """)
    List<StockAvailabilityRow> aggregateAvailability(
        @Param("productIds") List<String> productIds,
        @Param("locationIds") List<String> locationIds,
        @Param("status") com.springboot.erp.modules.inventory.stock.domain.StockStatus status);
}

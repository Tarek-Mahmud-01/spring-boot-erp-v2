package com.guru.erp.modules.procurement.bills.repository;

import com.guru.erp.modules.procurement.bills.domain.BillStatus;
import com.guru.erp.modules.procurement.bills.domain.SupplierBill;
import com.guru.erp.modules.procurement.bills.domain.SupplierBillLine;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link SupplierBill}. Soft-deleted rows are excluded automatically by the
 * {@code @SQLRestriction} on {@code BaseEntity}.
 */
public interface SupplierBillRepository extends JpaRepository<SupplierBill, Long> {

    Optional<SupplierBill> findByPublicId(String publicId);

    boolean existsByNumber(String number);

    /** Highest bill number for the BILL-NNNNN scheme (lexical == numeric with zero-pad). */
    @Query("select max(b.number) from SupplierBill b")
    String maxNumber();

    /** List with optional supplier, PO, status, and free-text (number OR supplier bill no OR notes) filters. */
    @Query("""
        select b from SupplierBill b
        where (:supplierId is null or b.supplierId = :supplierId)
          and (:poId is null or b.poId = :poId)
          and (:status is null or b.status = :status)
          and (:search is null
               or lower(b.number) like lower(concat('%', :search, '%'))
               or lower(b.supplierBillNo) like lower(concat('%', :search, '%'))
               or lower(b.notes) like lower(concat('%', :search, '%')))
        order by b.createdAt desc
        """)
    Page<SupplierBill> search(@Param("supplierId") String supplierId,
                              @Param("poId") String poId,
                              @Param("status") BillStatus status,
                              @Param("search") String search,
                              Pageable pageable);

    /** Σ billed total for a PO across non-Draft/non-Cancelled bills (the AP owed cap). */
    @Query("""
        select coalesce(sum(b.totalAmount), 0) from SupplierBill b
        where b.poId = :poId
          and b.status not in (com.guru.erp.modules.procurement.bills.domain.BillStatus.DRAFT,
                               com.guru.erp.modules.procurement.bills.domain.BillStatus.CANCELLED)
        """)
    long billedTotalForPo(@Param("poId") String poId);

    /** Cumulative billed qty for a PO line across all non-deleted bills (over-PO-qty guard). */
    @Query("""
        select coalesce(sum(l.qty), 0) from SupplierBillLine l
        where l.poLineId = :poLineId
        """)
    BigDecimal billedQtyForPoLine(@Param("poLineId") String poLineId);

    /** Every non-Draft/non-Cancelled bill on a PO — the set whose paid state is recomputed. */
    @Query("""
        select b from SupplierBill b
        where b.poId = :poId
          and b.status not in (com.guru.erp.modules.procurement.bills.domain.BillStatus.DRAFT,
                               com.guru.erp.modules.procurement.bills.domain.BillStatus.CANCELLED)
        """)
    java.util.List<SupplierBill> activeBillsForPo(@Param("poId") String poId);

    /** Delete all lines of a bill (update path replaces the full line set). */
    @org.springframework.data.jpa.repository.Modifying
    @Query("delete from SupplierBillLine l where l.bill.id = :billId")
    void deleteLinesByBillId(@Param("billId") Long billId);
}

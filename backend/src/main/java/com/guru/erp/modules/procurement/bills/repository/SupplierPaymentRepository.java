package com.guru.erp.modules.procurement.bills.repository;

import com.guru.erp.modules.procurement.bills.domain.SupplierPayment;
import com.guru.erp.modules.procurement.bills.domain.SupplierPaymentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link SupplierPayment}. Soft-deleted rows are auto-excluded by BaseEntity. */
public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {

    Optional<SupplierPayment> findByPublicId(String publicId);

    long count();

    @Query("""
        select p from SupplierPayment p
        where (:supplierId is null or p.supplierId = :supplierId)
          and (:poId is null or p.poId = :poId)
          and (:status is null or p.status = :status)
          and (:search is null
               or lower(p.number) like lower(concat('%', :search, '%'))
               or lower(p.invoiceReference) like lower(concat('%', :search, '%'))
               or lower(p.referenceNo) like lower(concat('%', :search, '%'))
               or lower(p.paymentMethodName) like lower(concat('%', :search, '%')))
        order by p.createdAt desc
        """)
    Page<SupplierPayment> search(@Param("supplierId") String supplierId,
                                 @Param("poId") String poId,
                                 @Param("status") SupplierPaymentStatus status,
                                 @Param("search") String search,
                                 Pageable pageable);

    /** Σ confirmed (Approved/Partially Paid/Paid) payment amounts for a PO. */
    @Query("""
        select coalesce(sum(p.amountAmount), 0) from SupplierPayment p
        where p.poId = :poId
          and p.status in (com.guru.erp.modules.procurement.bills.domain.SupplierPaymentStatus.APPROVED,
                           com.guru.erp.modules.procurement.bills.domain.SupplierPaymentStatus.PARTIALLY_PAID,
                           com.guru.erp.modules.procurement.bills.domain.SupplierPaymentStatus.PAID)
        """)
    long confirmedPaidForPo(@Param("poId") String poId);

    /** Active (non-Draft) payments on a PO — synced to the derived paid state. */
    @Query("""
        select p from SupplierPayment p
        where p.poId = :poId
          and p.status <> com.guru.erp.modules.procurement.bills.domain.SupplierPaymentStatus.DRAFT
        """)
    List<SupplierPayment> activePaymentsForPo(@Param("poId") String poId);
}

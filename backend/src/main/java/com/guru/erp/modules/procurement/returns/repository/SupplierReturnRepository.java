package com.guru.erp.modules.procurement.returns.repository;

import com.guru.erp.modules.procurement.returns.domain.ReturnStatus;
import com.guru.erp.modules.procurement.returns.domain.SupplierReturn;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link SupplierReturn}. Soft-deleted rows are excluded automatically by the
 * {@code @SQLRestriction} on {@code BaseEntity}.
 */
public interface SupplierReturnRepository extends JpaRepository<SupplierReturn, Long> {

    Optional<SupplierReturn> findByPublicId(String publicId);

    boolean existsByNumber(String number);

    long count();

    /** List with optional supplier, GRN, status, and free-text (number) filters. */
    @Query("""
        select r from SupplierReturn r
        where (:supplierId is null or r.supplierId = :supplierId)
          and (:grnId is null or r.grnId = :grnId)
          and (:status is null or r.status = :status)
          and (:search is null
               or lower(r.number) like lower(concat('%', :search, '%')))
        order by r.createdAt desc
        """)
    Page<SupplierReturn> search(@Param("supplierId") String supplierId,
                                @Param("grnId") String grnId,
                                @Param("status") ReturnStatus status,
                                @Param("search") String search,
                                Pageable pageable);

    /**
     * Sum of returned qty already booked against a GRN line across all live returns, optionally
     * excluding one return (so editing a return does not count its own current qty against itself).
     * Mirrors the reference "already returned" running total used to cap against received qty.
     */
    @Query("""
        select coalesce(sum(l.qty), 0)
        from SupplierReturnLine l
        where l.grnLineId = :grnLineId
          and (:excludeReturnId is null or l.supplierReturn.id <> :excludeReturnId)
        """)
    BigDecimal sumReturnedQtyForGrnLine(@Param("grnLineId") String grnLineId,
                                        @Param("excludeReturnId") Long excludeReturnId);
}

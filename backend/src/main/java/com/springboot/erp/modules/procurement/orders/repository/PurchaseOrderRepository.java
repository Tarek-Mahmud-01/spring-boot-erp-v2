package com.springboot.erp.modules.procurement.orders.repository;

import com.springboot.erp.modules.procurement.orders.domain.PoStatus;
import com.springboot.erp.modules.procurement.orders.domain.PurchaseOrder;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link PurchaseOrder}. Soft-deleted rows are excluded automatically by the
 * {@code @SQLRestriction} on {@code BaseEntity}.
 */
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    Optional<PurchaseOrder> findByPublicId(String publicId);

    boolean existsByNumber(String number);

    long countByNumberStartingWith(String prefix);

    /** List with optional supplier, location, status, direct-flag, and free-text filters. */
    @Query("""
        select po from PurchaseOrder po
        where (:supplierId is null or po.supplierId = :supplierId)
          and (:locationId is null or po.locationId = :locationId)
          and (:status is null or po.status = :status)
          and (:direct is null or po.direct = :direct)
          and (:search is null
               or lower(po.number) like lower(concat('%', cast(:search as string), '%'))
               or lower(po.notes) like lower(concat('%', cast(:search as string), '%')))
        order by po.createdAt desc
        """)
    Page<PurchaseOrder> search(@Param("supplierId") String supplierId,
                               @Param("locationId") String locationId,
                               @Param("status") PoStatus status,
                               @Param("direct") Boolean direct,
                               @Param("search") String search,
                               Pageable pageable);
}

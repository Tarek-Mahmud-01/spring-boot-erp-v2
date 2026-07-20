package com.springboot.erp.modules.procurement.orders.repository;

import com.springboot.erp.modules.procurement.orders.domain.PrStatus;
import com.springboot.erp.modules.procurement.orders.domain.PurchaseRequisition;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link PurchaseRequisition}. Soft-deleted rows are excluded automatically by the
 * {@code @SQLRestriction} on {@code BaseEntity}.
 */
public interface PurchaseRequisitionRepository extends JpaRepository<PurchaseRequisition, Long> {

    Optional<PurchaseRequisition> findByPublicId(String publicId);

    boolean existsByNumber(String number);

    long countByNumberStartingWith(String prefix);

    /** List with optional location, status, and free-text (number OR notes) filters. */
    @Query("""
        select pr from PurchaseRequisition pr
        where (:locationId is null or pr.locationId = :locationId)
          and (:status is null or pr.status = :status)
          and (:search is null
               or lower(pr.number) like lower(concat('%', cast(:search as string), '%'))
               or lower(pr.notes) like lower(concat('%', cast(:search as string), '%')))
        order by pr.id desc
        """)
    Page<PurchaseRequisition> search(@Param("locationId") String locationId,
                                     @Param("status") PrStatus status,
                                     @Param("search") String search,
                                     Pageable pageable);
}

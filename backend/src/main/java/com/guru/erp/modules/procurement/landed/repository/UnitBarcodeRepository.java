package com.guru.erp.modules.procurement.landed.repository;

import com.guru.erp.modules.procurement.landed.domain.UnitBarcode;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link UnitBarcode}. Soft-deleted rows are excluded automatically by the
 * {@code @SQLRestriction} on {@code BaseEntity}.
 */
public interface UnitBarcodeRepository extends JpaRepository<UnitBarcode, Long> {

    Optional<UnitBarcode> findByPublicId(String publicId);

    boolean existsByBarcode(String barcode);

    boolean existsByBarcodeAndPublicIdNot(String barcode, String publicId);

    /** Σ qty of every barcode already assigned to a GRN line — the consumed quota. */
    @Query("""
        select coalesce(sum(ub.qty), 0) from UnitBarcode ub
        where ub.grnLineId = :grnLineId
        """)
    BigDecimal sumQtyByGrnLineId(@Param("grnLineId") String grnLineId);

    /** List with optional grnLineId / grnId / product / bundle / status / free-text filters. */
    @Query("""
        select ub from UnitBarcode ub
        where (:grnLineId is null or ub.grnLineId = :grnLineId)
          and (:grnId is null or ub.grnId = :grnId)
          and (:productId is null or ub.productId = :productId)
          and (:bundle is null or ub.bundle = :bundle)
          and (:status is null or ub.status = :status)
          and (:search is null or lower(ub.barcode) like lower(concat('%', :search, '%')))
        order by ub.id desc
        """)
    Page<UnitBarcode> search(@Param("grnLineId") String grnLineId,
                             @Param("grnId") String grnId,
                             @Param("productId") String productId,
                             @Param("bundle") Boolean bundle,
                             @Param("status") com.guru.erp.modules.procurement.landed.domain.UnitBarcodeStatus status,
                             @Param("search") String search,
                             Pageable pageable);
}

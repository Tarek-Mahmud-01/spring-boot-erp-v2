package com.guru.erp.modules.procurement.receipts.repository;

import com.guru.erp.modules.procurement.receipts.domain.GoodsReceipt;
import com.guru.erp.modules.procurement.receipts.domain.GrnStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link GoodsReceipt}. Soft-deleted rows are excluded automatically by the
 * {@code @SQLRestriction} on {@code BaseEntity}.
 */
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {

    Optional<GoodsReceipt> findByPublicId(String publicId);

    boolean existsByNumber(String number);

    /**
     * List with optional PO, status, auto-receipt, and free-text (number) filters. Mirrors the
     * reference {@code list_grns} filters that are resolvable within this slice (supplier/PO-number
     * search lives cross-slice and is out of scope here).
     */
    @Query("""
        select g from GoodsReceipt g
        where (:poId is null or g.poId = :poId)
          and (:status is null or g.status = :status)
          and (:autoReceipt is null or g.autoReceipt = :autoReceipt)
          and (:search is null
               or lower(g.number) like lower(concat('%', :search, '%')))
        order by g.createdAt desc
        """)
    Page<GoodsReceipt> search(@Param("poId") String poId,
                              @Param("status") GrnStatus status,
                              @Param("autoReceipt") Boolean autoReceipt,
                              @Param("search") String search,
                              Pageable pageable);
}

package com.springboot.erp.modules.procurement.landed.repository;

import com.springboot.erp.modules.procurement.landed.domain.LandedCost;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link LandedCost}. Soft-deleted rows are excluded automatically by the
 * {@code @SQLRestriction} on {@code BaseEntity}.
 */
public interface LandedCostRepository extends JpaRepository<LandedCost, Long> {

    Optional<LandedCost> findByPublicId(String publicId);

    List<LandedCost> findByInvoiceNumberOrderByIdAsc(String invoiceNumber);

    List<LandedCost> findByGrnIdOrderByIdAsc(String grnId);

    boolean existsByInvoiceNumber(String invoiceNumber);

    /** Highest existing invoice number for the year's LC-YYYY- prefix (lexical = numeric order). */
    @Query("""
        select max(lc.invoiceNumber) from LandedCost lc
        where lc.invoiceNumber like concat(:prefix, '%')
        """)
    String maxInvoiceNumberForPrefix(@Param("prefix") String prefix);

    /** List with optional GRN scope + free-text (invoice number OR charge type) filters. */
    @Query("""
        select lc from LandedCost lc
        where (:grnId is null or lc.grnId = :grnId)
          and (:search is null
               or lower(lc.invoiceNumber) like lower(concat('%', cast(:search as string), '%'))
               or lower(cast(lc.chargeType as string)) like lower(concat('%', cast(:search as string), '%')))
        order by lc.id desc
        """)
    Page<LandedCost> search(@Param("grnId") String grnId,
                            @Param("search") String search,
                            Pageable pageable);
}

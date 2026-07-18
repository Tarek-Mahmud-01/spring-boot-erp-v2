package com.guru.erp.modules.product.pricing.repository;

import com.guru.erp.modules.product.pricing.domain.PriceHistory;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for the append-only {@link PriceHistory} ledger. Only reads and
 * inserts — rows are never updated or deleted.
 */
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    Optional<PriceHistory> findByPublicId(String publicId);

    /** AC-013-4 — a product's price history, newest effective-date first. */
    @Query("""
        select h from PriceHistory h
        where h.productId = :productId
        order by h.effectiveFrom desc, h.id desc
        """)
    Page<PriceHistory> findByProductId(@Param("productId") String productId, Pageable pageable);
}

package com.springboot.erp.modules.inventory.stock.repository;

import com.springboot.erp.modules.inventory.stock.domain.ProductBatch;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for {@link ProductBatch}. Barcode look-up backs the POS scan-time
 * batch resolution; product scoping backs the batch list. {@code @SQLRestriction}
 * excludes soft-deleted rows automatically.
 */
public interface ProductBatchRepository extends JpaRepository<ProductBatch, Long> {

    Optional<ProductBatch> findByPublicId(String publicId);

    Optional<ProductBatch> findByBarcode(String barcode);

    boolean existsByBarcode(String barcode);

    Page<ProductBatch> findByProductId(String productId, Pageable pageable);
}

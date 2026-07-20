package com.springboot.erp.modules.product.catalog.repository;

import com.springboot.erp.modules.product.catalog.domain.ProductBarcode;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductBarcodeRepository extends JpaRepository<ProductBarcode, Long> {

    Optional<ProductBarcode> findByPublicId(String publicId);

    Optional<ProductBarcode> findByBarcode(String barcode);

    boolean existsByBarcode(String barcode);

    List<ProductBarcode> findByProductIdOrderById(Long productId);

    List<ProductBarcode> findByProductIdAndIsPrimaryTrue(Long productId);

    boolean existsByVariantId(Long variantId);
}

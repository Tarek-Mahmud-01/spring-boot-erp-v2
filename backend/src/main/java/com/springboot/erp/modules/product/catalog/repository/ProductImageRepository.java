package com.springboot.erp.modules.product.catalog.repository;

import com.springboot.erp.modules.product.catalog.domain.ProductImage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    Optional<ProductImage> findByPublicId(String publicId);

    List<ProductImage> findByProductIdOrderByPosition(Long productId);

    long countByProductId(Long productId);
}

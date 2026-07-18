package com.guru.erp.modules.product.catalog.repository;

import com.guru.erp.modules.product.catalog.domain.ProductVariant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    Optional<ProductVariant> findByPublicId(String publicId);

    List<ProductVariant> findByParentProductIdOrderById(Long parentProductId);

    boolean existsBySkuIgnoreCase(String sku);

    @Query("""
        select (count(v) > 0) from ProductVariant v
        where lower(v.sku) = lower(:sku) and v.id <> :excludeId
        """)
    boolean existsBySkuIgnoreCaseAndIdNot(@Param("sku") String sku, @Param("excludeId") Long excludeId);

    long countByParentProductIdAndIdNot(Long parentProductId, Long excludeId);
}

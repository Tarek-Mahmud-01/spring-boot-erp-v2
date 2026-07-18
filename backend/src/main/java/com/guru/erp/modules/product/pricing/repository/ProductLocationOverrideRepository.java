package com.guru.erp.modules.product.pricing.repository;

import com.guru.erp.modules.product.pricing.domain.ProductLocationOverride;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link ProductLocationOverride}. */
public interface ProductLocationOverrideRepository
        extends JpaRepository<ProductLocationOverride, Long> {

    Optional<ProductLocationOverride> findByPublicId(String publicId);

    /** FR-063 — a product's location overrides, oldest first (stable order). */
    @Query("""
        select o from ProductLocationOverride o
        where o.productId = :productId
        order by o.id
        """)
    Page<ProductLocationOverride> findByProductId(@Param("productId") String productId,
                                                  Pageable pageable);

    /**
     * The live override for a (product, location, variant) triple, matching the
     * partial unique key. Null-variant handled explicitly.
     */
    @Query("""
        select o from ProductLocationOverride o
        where o.productId = :productId
          and o.locationId = :locationId
          and (:variantId is null and o.variantId is null or o.variantId = :variantId)
        """)
    Optional<ProductLocationOverride> findExisting(@Param("productId") String productId,
                                                   @Param("locationId") String locationId,
                                                   @Param("variantId") String variantId);
}

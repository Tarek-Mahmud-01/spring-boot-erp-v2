package com.guru.erp.modules.product.catalog.repository;

import com.guru.erp.modules.product.catalog.domain.LifecycleState;
import com.guru.erp.modules.product.catalog.domain.Product;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link Product}. The {@code @SQLRestriction} on
 * {@code BaseEntity} already excludes soft-deleted rows from every query.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByPublicId(String publicId);

    /** FR-055 — case-insensitive SKU uniqueness among live products. */
    boolean existsBySkuIgnoreCase(String sku);

    /** FR-283 / AC-270-2 — PLU uniqueness among live products. */
    boolean existsByPlu(String plu);

    /** FR-284 — resolve a decoded variable-measure PLU to its weighed product. */
    @Query("select p from Product p where p.plu = :plu and p.soldByWeight = true")
    Optional<Product> findByPluAndSoldByWeight(@Param("plu") String plu);

    @Query("""
        select (count(p) > 0) from Product p
        where lower(p.sku) = lower(:sku) and p.id <> :excludeId
        """)
    boolean existsBySkuIgnoreCaseAndIdNot(@Param("sku") String sku, @Param("excludeId") Long excludeId);

    @Query("""
        select (count(p) > 0) from Product p
        where p.plu = :plu and p.id <> :excludeId
        """)
    boolean existsByPluAndIdNot(@Param("plu") String plu, @Param("excludeId") Long excludeId);

    @Query("""
        select p from Product p
        where (:q is null
               or lower(p.sku) like lower(concat('%', :q, '%'))
               or lower(p.name) like lower(concat('%', :q, '%')))
          and (:lifecycleState is null or p.lifecycleState = :lifecycleState)
          and (:isActive is null or p.isActive = :isActive)
        order by p.name
        """)
    Page<Product> search(@Param("q") String q,
                         @Param("lifecycleState") LifecycleState lifecycleState,
                         @Param("isActive") Boolean isActive,
                         Pageable pageable);
}

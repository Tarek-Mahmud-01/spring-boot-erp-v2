package com.springboot.erp.modules.product.pricing.repository;

import com.springboot.erp.modules.product.pricing.domain.PriceListItem;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link PriceListItem}. */
public interface PriceListItemRepository extends JpaRepository<PriceListItem, Long> {

    Optional<PriceListItem> findByPublicId(String publicId);

    /** Items on a price list, newest effective-date first (list-detail view). */
    @Query("""
        select i from PriceListItem i
        where i.priceList.id = :priceListId
        order by i.effectiveFrom desc, i.id desc
        """)
    Page<PriceListItem> findByPriceListId(@Param("priceListId") long priceListId, Pageable pageable);

    /**
     * FR-062 scheduling key: an exact match on (list, product, variant,
     * effectiveFrom). Handles the null-variant case explicitly since SQL
     * {@code = null} never matches.
     */
    @Query("""
        select i from PriceListItem i
        where i.priceList.id = :priceListId
          and i.productId = :productId
          and (:variantId is null and i.variantId is null or i.variantId = :variantId)
          and i.effectiveFrom = :effectiveFrom
        """)
    Optional<PriceListItem> findScheduled(@Param("priceListId") long priceListId,
                                          @Param("productId") String productId,
                                          @Param("variantId") String variantId,
                                          @Param("effectiveFrom") Instant effectiveFrom);

    /**
     * FR-061 "one current price per product per list": the most-recently created
     * row for (list, product, variant), regardless of effective date.
     */
    @Query("""
        select i from PriceListItem i
        where i.priceList.id = :priceListId
          and i.productId = :productId
          and (:variantId is null and i.variantId is null or i.variantId = :variantId)
        order by i.id desc
        """)
    Page<PriceListItem> findLatestForProduct(@Param("priceListId") long priceListId,
                                             @Param("productId") String productId,
                                             @Param("variantId") String variantId,
                                             Pageable pageable);
}

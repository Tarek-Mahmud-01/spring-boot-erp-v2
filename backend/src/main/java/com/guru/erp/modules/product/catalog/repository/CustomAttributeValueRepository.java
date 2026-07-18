package com.guru.erp.modules.product.catalog.repository;

import com.guru.erp.modules.product.catalog.domain.CustomAttributeValue;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomAttributeValueRepository extends JpaRepository<CustomAttributeValue, Long> {

    Optional<CustomAttributeValue> findByPublicId(String publicId);

    List<CustomAttributeValue> findByProductIdOrderById(Long productId);

    Optional<CustomAttributeValue> findByProductIdAndVariantIdAndScopeAndKey(
        Long productId, Long variantId, String scope, String key);
}

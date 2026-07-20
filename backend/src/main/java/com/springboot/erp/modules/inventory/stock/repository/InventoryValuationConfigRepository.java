package com.springboot.erp.modules.inventory.stock.repository;

import com.springboot.erp.modules.inventory.stock.domain.InventoryValuationConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for {@link InventoryValuationConfig}. Exactly one live row per
 * company, keyed by the loose {@code companyId} ULID.
 */
public interface InventoryValuationConfigRepository
    extends JpaRepository<InventoryValuationConfig, Long> {

    Optional<InventoryValuationConfig> findByPublicId(String publicId);

    Optional<InventoryValuationConfig> findByCompanyId(String companyId);
}

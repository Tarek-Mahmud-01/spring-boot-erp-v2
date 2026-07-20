package com.springboot.erp.modules.crm.loyalty.repository;

import com.springboot.erp.modules.crm.loyalty.domain.LoyaltyConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for the per-company {@link LoyaltyConfig} singleton. */
public interface LoyaltyConfigRepository extends JpaRepository<LoyaltyConfig, Long> {

    Optional<LoyaltyConfig> findByPublicId(String publicId);

    Optional<LoyaltyConfig> findByCompanyId(String companyId);
}

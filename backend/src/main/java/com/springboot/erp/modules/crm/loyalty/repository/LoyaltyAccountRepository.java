package com.springboot.erp.modules.crm.loyalty.repository;

import com.springboot.erp.modules.crm.loyalty.domain.LoyaltyAccount;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for the per-customer {@link LoyaltyAccount} (1:1 with customer). */
public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Long> {

    Optional<LoyaltyAccount> findByPublicId(String publicId);

    Optional<LoyaltyAccount> findByCustomerId(String customerId);

    Page<LoyaltyAccount> findByTierId(String tierId, Pageable pageable);

    Page<LoyaltyAccount> findAll(Pageable pageable);
}

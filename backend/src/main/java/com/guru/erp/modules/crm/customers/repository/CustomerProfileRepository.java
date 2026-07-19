package com.guru.erp.modules.crm.customers.repository;

import com.guru.erp.modules.crm.customers.domain.CustomerProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link CustomerProfile} (1:1 child of Customer). */
public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {

    Optional<CustomerProfile> findByCustomerId(Long customerId);
}

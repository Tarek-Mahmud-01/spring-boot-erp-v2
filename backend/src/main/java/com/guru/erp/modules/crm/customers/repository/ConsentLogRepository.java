package com.guru.erp.modules.crm.customers.repository;

import com.guru.erp.modules.crm.customers.domain.ConsentLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for the append-only {@link ConsentLog} (FR-201 audit trail).
 * The service layer is the only writer; rows are never updated or deleted.
 */
public interface ConsentLogRepository extends JpaRepository<ConsentLog, Long> {

    Page<ConsentLog> findByCustomerIdOrderByRecordedAtDesc(Long customerId, Pageable pageable);
}

package com.guru.erp.platform.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** The most recent audit row's hash — the chain head to link the next row to. */
    AuditLog findTopByOrderByIdDesc();
}

package com.springboot.erp.platform.audit;

/** The mutation kinds that produce exactly one audit row each (ARCHITECTURE.md §2). */
public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    RESTORE
}

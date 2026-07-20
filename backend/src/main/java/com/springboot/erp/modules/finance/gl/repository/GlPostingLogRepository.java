package com.springboot.erp.modules.finance.gl.repository;

import com.springboot.erp.modules.finance.gl.domain.GlPostingLog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link GlPostingLog} — the idempotency ledger for automated postings. */
public interface GlPostingLogRepository extends JpaRepository<GlPostingLog, Long> {

    /** The natural idempotency key: one row per (sourceKind, sourceRef). */
    Optional<GlPostingLog> findBySourceKindAndSourceRef(String sourceKind, String sourceRef);
}

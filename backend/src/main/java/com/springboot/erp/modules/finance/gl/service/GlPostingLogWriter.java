package com.springboot.erp.modules.finance.gl.service;

import com.springboot.erp.modules.finance.gl.domain.GlPostingLog;
import com.springboot.erp.modules.finance.gl.domain.GlPostingStatus;
import com.springboot.erp.modules.finance.gl.mapper.GlMapper;
import com.springboot.erp.modules.finance.gl.repository.GlPostingLogRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import org.springframework.stereotype.Component;

/**
 * Writes (or updates in place) the {@link GlPostingLog} idempotency row for
 * {@link GlPostingConsumerService} — split out purely to keep that service under the size cap.
 * Reference {@code _record_log}: a second INSERT for the same {@code (sourceKind, sourceRef)}
 * would violate the unique index (V71), so a replay that finds ANY prior row — even a FAILED one
 * from a previous attempt — updates that row in place rather than inserting a new one.
 */
@Component
class GlPostingLogWriter {

    private static final String AUDIT_ENTITY = "gl_posting_log";

    private final GlPostingLogRepository repository;
    private final GlMapper mapper;
    private final AuditService auditService;

    GlPostingLogWriter(GlPostingLogRepository repository, GlMapper mapper, AuditService auditService) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    GlPostingLog record(GlPostingLog existing, String companyId, String sourceKind, String sourceRef,
                        String eventType, GlPostingStatus status, String journalEntryId, String requestId) {
        if (existing != null) {
            existing.setStatus(status);
            existing.setJournalEntryId(journalEntryId);
            existing.setRequestId(requestId);
            existing.setAttempts(existing.getAttempts() + 1);
            GlPostingLog saved = repository.save(existing);
            auditService.record(AUDIT_ENTITY, sourceRef, AuditAction.UPDATE, null, mapper.toResponse(saved));
            return saved;
        }
        GlPostingLog row = new GlPostingLog();
        row.setCompanyId(companyId);
        row.setSourceKind(sourceKind);
        row.setSourceRef(sourceRef);
        row.setEventType(eventType);
        row.setJournalEntryId(journalEntryId);
        row.setStatus(status);
        row.setAttempts(1);
        row.setRequestId(requestId);
        GlPostingLog saved = repository.save(row);
        auditService.record(AUDIT_ENTITY, sourceRef, AuditAction.CREATE, null, mapper.toResponse(saved));
        return saved;
    }

    void recordFailure(GlPostingLog existing, String companyId, String sourceKind, String sourceRef,
                       String eventType, RuntimeException ex, String requestId) {
        GlPostingLog row = existing != null ? existing : new GlPostingLog();
        if (existing == null) {
            row.setCompanyId(companyId);
            row.setSourceKind(sourceKind);
            row.setSourceRef(sourceRef);
            row.setEventType(eventType);
        }
        row.setStatus(GlPostingStatus.FAILED);
        row.setAttempts(row.getAttempts() + 1);
        row.setLastError(ex.getMessage());
        row.setRequestId(requestId);
        repository.save(row);
    }
}

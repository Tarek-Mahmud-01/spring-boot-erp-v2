package com.springboot.erp.modules.finance.gl.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Idempotency + reconciliation ledger for automated GL postings (reference
 * {@code app.finance.models.GlPostingLog}). One row per source event that should produce a GL
 * voucher (a POS sale, refund, void, till deposit, ...). The natural idempotency key is
 * {@code (sourceKind, sourceRef)} — enforced by a unique index in V71 — where {@code sourceRef} is
 * the source aggregate's public id (e.g. the {@code pos_transaction.publicId}) and
 * {@code sourceKind} classifies which kind of source document it is.
 *
 * <p>Because the outbox is at-least-once, {@code GlPostingConsumerService} MUST check-then-insert
 * this row transactionally (same transaction as the journal-entry write) before applying any
 * journal effects, so replaying {@code pos.sale.completed} twice never double-posts. A replay that
 * finds an existing POSTED/SKIPPED row short-circuits; a replay that finds a FAILED row retries and
 * updates the same row in place (never a second INSERT, which would violate the unique index).
 */
@Entity
@Table(name = "gl_posting_log")
public class GlPostingLog extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "company_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String companyId;

    /** e.g. {@code "POS_SALE"} / {@code "POS_REFUND"} / {@code "POS_VOID"} / {@code "POS_TILL_DEPOSIT"}. */
    @Column(name = "source_kind", nullable = false, length = 24)
    private String sourceKind;

    /** Public id of the source aggregate (e.g. the pos_transaction that produced this event). */
    @Column(name = "source_ref", nullable = false, length = 26)
    private String sourceRef;

    /** The outbox event type that drove this posting attempt, e.g. {@code "pos.sale.completed"}. */
    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    /** Loose reference to the JournalEntry this attempt produced; null when SKIPPED/FAILED. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "journal_entry_id", length = 26, columnDefinition = "char(26)")
    private String journalEntryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private GlPostingStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "request_id", length = 64)
    private String requestId;

    public GlPostingLog() {
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getSourceKind() {
        return sourceKind;
    }

    public void setSourceKind(String sourceKind) {
        this.sourceKind = sourceKind;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getJournalEntryId() {
        return journalEntryId;
    }

    public void setJournalEntryId(String journalEntryId) {
        this.journalEntryId = journalEntryId;
    }

    public GlPostingStatus getStatus() {
        return status;
    }

    public void setStatus(GlPostingStatus status) {
        this.status = status;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}

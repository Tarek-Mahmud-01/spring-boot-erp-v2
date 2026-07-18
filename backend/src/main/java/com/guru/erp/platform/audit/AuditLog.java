package com.guru.erp.platform.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Append-only, hash-chained audit record (ARCHITECTURE.md §2). One row per
 * CREATE/UPDATE/DELETE/RESTORE, carrying before/after JSONB, the actor, and the
 * request id. {@code rowHash = sha256(prevHash + canonical(row))} links each
 * row to its predecessor; the DB REVOKEs UPDATE/DELETE so the chain is tamper
 * evident. Deliberately does NOT extend BaseEntity — it is not soft-deletable,
 * not versioned, never mutated.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "entity_public_id", length = 26, columnDefinition = "char(26)")
    private String entityPublicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 16)
    private AuditAction action;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "actor_public_id", length = 26, columnDefinition = "char(26)")
    private String actorPublicId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "request_id", length = 26, columnDefinition = "char(26)")
    private String requestId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_data", columnDefinition = "jsonb")
    private String beforeData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_data", columnDefinition = "jsonb")
    private String afterData;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "prev_hash", length = 64, columnDefinition = "char(64)")
    private String prevHash;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "row_hash", nullable = false, length = 64, columnDefinition = "char(64)")
    private String rowHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditLog() {
    }

    AuditLog(String entityType, String entityPublicId, AuditAction action, String actorPublicId,
             String requestId, String beforeData, String afterData, String prevHash, String rowHash,
             Instant createdAt) {
        this.entityType = entityType;
        this.entityPublicId = entityPublicId;
        this.action = action;
        this.actorPublicId = actorPublicId;
        this.requestId = requestId;
        this.beforeData = beforeData;
        this.afterData = afterData;
        this.prevHash = prevHash;
        this.rowHash = rowHash;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getRowHash() {
        return rowHash;
    }

    public String getPrevHash() {
        return prevHash;
    }

    public String getEntityType() {
        return entityType;
    }

    public AuditAction getAction() {
        return action;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

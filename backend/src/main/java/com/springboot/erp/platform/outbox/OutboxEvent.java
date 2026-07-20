package com.springboot.erp.platform.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Transactional outbox row (ARCHITECTURE.md §2 — POS→GL→Inv→CRM event bus).
 * Written in the same transaction as the state change that produced it, then a
 * relay publishes it and marks it processed — guaranteeing at-least-once
 * delivery without a distributed transaction.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", nullable = false, updatable = false, length = 26, columnDefinition = "char(26)", unique = true)
    private String publicId;

    /** Domain aggregate that emitted this, e.g. {@code "sales.Invoice"}. */
    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "aggregate_public_id", length = 26, columnDefinition = "char(26)")
    private String aggregatePublicId;

    /** Event name, e.g. {@code "InvoicePosted"}. */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    protected OutboxEvent() {
    }

    OutboxEvent(String publicId, String aggregateType, String aggregatePublicId, String eventType,
                String payload, Instant createdAt) {
        this.publicId = publicId;
        this.aggregateType = aggregateType;
        this.aggregatePublicId = aggregatePublicId;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = createdAt;
        this.attempts = 0;
    }

    public void markProcessed(Instant when) {
        this.processedAt = when;
    }

    public void incrementAttempts() {
        this.attempts++;
    }

    public Long getId() {
        return id;
    }

    public String getPublicId() {
        return publicId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregatePublicId() {
        return aggregatePublicId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public int getAttempts() {
        return attempts;
    }
}

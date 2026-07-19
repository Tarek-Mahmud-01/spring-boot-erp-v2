package com.guru.erp.modules.crm.customers.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Append-only history of every consent toggle (reference {@code ConsentLog},
 * FR-201 audit trail) — similar spirit to the platform hash-chained audit log,
 * but domain-specific and queried directly for the customer's consent
 * history. Rows are never updated or deleted by application code; the service
 * layer is the only writer.
 *
 * <p>Same-slice child of {@link Customer} — real FK, cascade delete.
 */
@Entity
@Table(name = "customer_consent_log")
public class ConsentLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 8)
    private ConsentChannel channel;

    @Column(name = "granted", nullable = false)
    private boolean granted;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    /** Loose ref to the capturing user (access.user) — no hard FK. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "recorded_by", length = 26, columnDefinition = "char(26)")
    private String recordedBy;

    public ConsentLog() {
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public ConsentChannel getChannel() {
        return channel;
    }

    public void setChannel(ConsentChannel channel) {
        this.channel = channel;
    }

    public boolean isGranted() {
        return granted;
    }

    public void setGranted(boolean granted) {
        this.granted = granted;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(String recordedBy) {
        this.recordedBy = recordedBy;
    }
}

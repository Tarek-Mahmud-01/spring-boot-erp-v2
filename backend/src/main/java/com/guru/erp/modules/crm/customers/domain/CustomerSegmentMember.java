package com.guru.erp.modules.crm.customers.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * ENT-073a CustomerSegmentMember — a customer's membership row in a
 * {@link CustomerSegment} (reference {@code CustomerSegmentMember}).
 * Same-slice child of both {@link CustomerSegment} (real FK, cascade delete)
 * and {@link Customer} (real FK, cascade delete). Constraint reproduced in
 * V60: unique {@code (segment_id, customer_id)}.
 */
@Entity
@Table(name = "customer_segment_members", uniqueConstraints = @UniqueConstraint(name = "uq_customer_segment_members_unique", columnNames = {"segment_id", "customer_id"}))
public class CustomerSegmentMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "segment_id", nullable = false)
    private CustomerSegment segment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    public CustomerSegmentMember() {
    }

    public CustomerSegment getSegment() {
        return segment;
    }

    public void setSegment(CustomerSegment segment) {
        this.segment = segment;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Instant getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Instant addedAt) {
        this.addedAt = addedAt;
    }
}

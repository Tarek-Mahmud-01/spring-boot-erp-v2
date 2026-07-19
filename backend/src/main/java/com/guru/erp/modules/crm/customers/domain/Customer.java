package com.guru.erp.modules.crm.customers.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-050 Customer — core record (reference {@code Customer}, US-038 /
 * FR-199-204). {@code companyId} is a loose cross-module ULID reference
 * (settings.company) — no hard FK, per the vertical-slice rule.
 *
 * <p>Constraints reproduced in V60: unique {@code membership_id}, {@code type}
 * / {@code status} checks. {@link CustomerProfile} is the 1:1 CRM contact +
 * consent extension, same-slice child with cascade delete.
 */
@Entity
@Table(name = "customers", uniqueConstraints = @UniqueConstraint(name = "uq_customers_membership_id", columnNames = "membership_id"))
public class Customer extends BaseEntity {

    /** Loose cross-module ULID ref (settings.company) — no hard FK. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "company_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String companyId;

    /** FR-210 — short, scannable, unique-per-tenant id used by POS lookup. */
    @Column(name = "membership_id", nullable = false, length = 20)
    private String membershipId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private CustomerType type = CustomerType.INDIVIDUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private CustomerStatus status = CustomerStatus.ACTIVE;

    /** FR-218 — when false, analytics-derived fields are hidden from the detail view. */
    @Column(name = "analytics_consent", nullable = false)
    private boolean analyticsConsent = false;

    /** Unbounded — avatars arrive as base64 {@code data:} URLs (no object store wired yet). */
    @Column(name = "photo_url", columnDefinition = "text")
    private String photoUrl;

    /** FR-203 — anonymization end-state; the row is preserved so history keeps its reference. */
    @Column(name = "anonymized", nullable = false)
    private boolean anonymized = false;

    @Column(name = "anonymized_at")
    private Instant anonymizedAt;

    @OneToOne(mappedBy = "customer", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private CustomerProfile profile;

    public Customer() {
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(String membershipId) {
        this.membershipId = membershipId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public CustomerType getType() {
        return type;
    }

    public void setType(CustomerType type) {
        this.type = type;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public void setStatus(CustomerStatus status) {
        this.status = status;
    }

    public boolean isAnalyticsConsent() {
        return analyticsConsent;
    }

    public void setAnalyticsConsent(boolean analyticsConsent) {
        this.analyticsConsent = analyticsConsent;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public boolean isAnonymized() {
        return anonymized;
    }

    public void setAnonymized(boolean anonymized) {
        this.anonymized = anonymized;
    }

    public Instant getAnonymizedAt() {
        return anonymizedAt;
    }

    public void setAnonymizedAt(Instant anonymizedAt) {
        this.anonymizedAt = anonymizedAt;
    }

    public CustomerProfile getProfile() {
        return profile;
    }

    public void setProfile(CustomerProfile profile) {
        this.profile = profile;
        if (profile != null) {
            profile.setCustomer(this);
        }
    }
}

package com.springboot.erp.modules.crm.customers.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-051 CustomerProfile — CRM contact + per-channel consent extension, 1:1
 * with {@link Customer} (reference {@code CustomerProfile}).
 *
 * <p>{@code preferredLocationId} is a loose cross-module ULID reference
 * (settings.location) — no hard FK. Constraints reproduced in V60: partial
 * unique indexes on {@code mobile} / {@code email} (each unique per tenant
 * when present, FR-200).
 */
@Entity
@Table(name = "customer_profiles")
public class CustomerProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private Customer customer;

    /** FR-200 — mobile OR email required; each unique per tenant when present. */
    @Column(name = "mobile", length = 32)
    private String mobile;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "postcode", length = 16)
    private String postcode;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /** Loose cross-module ULID ref (settings.location) — no hard FK. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "preferred_location_id", length = 26, columnDefinition = "char(26)")
    private String preferredLocationId;

    /** FR-201 — per-channel consent; defaults OFF (no pre-ticked boxes). */
    @Column(name = "consent_email", nullable = false)
    private boolean consentEmail = false;

    @Column(name = "consent_sms", nullable = false)
    private boolean consentSms = false;

    @Column(name = "consent_email_at")
    private Instant consentEmailAt;

    @Column(name = "consent_sms_at")
    private Instant consentSmsAt;

    /** Loose ref to the capturing user (access.user) — no hard FK. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "consent_email_by", length = 26, columnDefinition = "char(26)")
    private String consentEmailBy;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "consent_sms_by", length = 26, columnDefinition = "char(26)")
    private String consentSmsBy;

    public CustomerProfile() {
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getPreferredLocationId() {
        return preferredLocationId;
    }

    public void setPreferredLocationId(String preferredLocationId) {
        this.preferredLocationId = preferredLocationId;
    }

    public boolean isConsentEmail() {
        return consentEmail;
    }

    public void setConsentEmail(boolean consentEmail) {
        this.consentEmail = consentEmail;
    }

    public boolean isConsentSms() {
        return consentSms;
    }

    public void setConsentSms(boolean consentSms) {
        this.consentSms = consentSms;
    }

    public Instant getConsentEmailAt() {
        return consentEmailAt;
    }

    public void setConsentEmailAt(Instant consentEmailAt) {
        this.consentEmailAt = consentEmailAt;
    }

    public Instant getConsentSmsAt() {
        return consentSmsAt;
    }

    public void setConsentSmsAt(Instant consentSmsAt) {
        this.consentSmsAt = consentSmsAt;
    }

    public String getConsentEmailBy() {
        return consentEmailBy;
    }

    public void setConsentEmailBy(String consentEmailBy) {
        this.consentEmailBy = consentEmailBy;
    }

    public String getConsentSmsBy() {
        return consentSmsBy;
    }

    public void setConsentSmsBy(String consentSmsBy) {
        this.consentSmsBy = consentSmsBy;
    }
}

package com.guru.erp.modules.settings.taxcode.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ENT-007 TaxCode (US-003 / FR-012-016).
 *
 * <p>Tax codes are scoped to a single company via {@code companyPublicId} (the
 * company's ULID external identifier — the v2 companies module is not yet
 * ported, so the parent is referenced by its public id rather than an internal
 * FK). Multiple effective ranges per code are allowed (FR-014: a rate change
 * mints a new range); the service layer rejects overlapping effective ranges
 * for the same {@code (company, code)} pair (the reference's Postgres GIST
 * exclude constraint, enforced in Java).
 *
 * <p>Domain columns / constraints reproduced from the reference (mirror these in
 * the Flyway migration):
 * <ul>
 *   <li>unique {@code (company_public_id, code, effective_from)}</li>
 *   <li>check {@code rate_percent between 0 and 100}</li>
 *   <li>check {@code effective_to is null or effective_to >= effective_from}</li>
 *   <li>check {@code gst_treatment in (STANDARD, GST_FREE, EXPORT, INPUT_TAXED)}</li>
 * </ul>
 */
@Entity
@Table(
    name = "tax_codes",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_tax_codes_company_code_effective_from",
        columnNames = {"company_public_id", "code", "effective_from"}))
public class TaxCode extends BaseEntity {

    @Column(name = "company_public_id", nullable = false, length = 26)
    private String companyPublicId;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "description", nullable = false, length = 200)
    private String description;

    @Column(name = "rate_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal ratePercent;

    @Column(name = "is_inclusive", nullable = false)
    private boolean inclusive = false;

    @Column(name = "is_exempt", nullable = false)
    private boolean exempt = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "gst_treatment", nullable = false, length = 20)
    private GstTreatment gstTreatment = GstTreatment.STANDARD;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    protected TaxCode() {
    }

    public TaxCode(String companyPublicId, String code, String description, BigDecimal ratePercent,
                   boolean inclusive, boolean exempt, GstTreatment gstTreatment,
                   LocalDate effectiveFrom, LocalDate effectiveTo) {
        this.companyPublicId = companyPublicId;
        this.code = code;
        this.description = description;
        this.ratePercent = ratePercent;
        this.inclusive = inclusive;
        this.exempt = exempt;
        this.gstTreatment = gstTreatment;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
    }

    public String getCompanyPublicId() {
        return companyPublicId;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getRatePercent() {
        return ratePercent;
    }

    public void setRatePercent(BigDecimal ratePercent) {
        this.ratePercent = ratePercent;
    }

    public boolean isInclusive() {
        return inclusive;
    }

    public void setInclusive(boolean inclusive) {
        this.inclusive = inclusive;
    }

    public boolean isExempt() {
        return exempt;
    }

    public void setExempt(boolean exempt) {
        this.exempt = exempt;
    }

    public GstTreatment getGstTreatment() {
        return gstTreatment;
    }

    public void setGstTreatment(GstTreatment gstTreatment) {
        this.gstTreatment = gstTreatment;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    /** Derived status: "Inactive" once the range has closed in the past, else "Active". */
    public String status() {
        return (effectiveTo != null && effectiveTo.isBefore(LocalDate.now())) ? "Inactive" : "Active";
    }
}

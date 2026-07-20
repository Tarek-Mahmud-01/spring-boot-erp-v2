package com.springboot.erp.modules.finance.periods.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-AU-001 BasPeriod (reference {@code app.finance.models_bas.BasPeriod}, US-AU-005) — one BAS
 * (Business Activity Statement) lodgement-period instance for a company. Lifecycle OPEN -&gt; LODGED
 * -&gt; FROZEN (FR-AU-023) driven by {@code BasPeriodService} via the platform
 * {@link com.springboot.erp.platform.status.StateMachine}.
 *
 * <p>{@code companyId} is a loose cross-module ULID reference (settings.company) — no hard FK, per
 * the vertical-slice rule. {@code lodgementReference} is the ATO lodgement receipt/reference the
 * accountant records after manually filing the exported CSV via the ATO BAS portal (this slice
 * generates the report + CSV; actual filing stays a manual out-of-band step, reference INT-AU-001).
 */
@Entity
@Table(name = "bas_periods")
public class BasPeriod extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "company_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String companyId;

    /** e.g. "2026-Q1", "2026-07", "2026". */
    @Column(name = "period_code", nullable = false, length = 20)
    private String periodCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 16)
    private BasPeriodType periodType;

    @Column(name = "date_from", nullable = false)
    private LocalDate dateFrom;

    @Column(name = "date_to", nullable = false)
    private LocalDate dateTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private BasPeriodStatus status = BasPeriodStatus.OPEN;

    @Column(name = "lodged_at")
    private Instant lodgedAt;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "lodged_by", length = 26, columnDefinition = "char(26)")
    private String lodgedBy;

    @Column(name = "lodgement_reference", length = 120)
    private String lodgementReference;

    /**
     * The company's GL account used for GST/GST-payable postings — resolved explicitly at BasPeriod
     * creation time rather than via the (not-yet-ported) AccountMapping lookup the reference uses,
     * so this slice stays decoupled from the coa sub-slice's mapping table. Drives the 1A/1B split
     * in {@code BasReportGenerationService}.
     */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "gst_account_id", length = 26, columnDefinition = "char(26)")
    private String gstAccountId;

    /** The company's GL revenue account — drives G1 (total sales). Same rationale as {@code gstAccountId}. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "revenue_account_id", length = 26, columnDefinition = "char(26)")
    private String revenueAccountId;

    public BasPeriod() {
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getPeriodCode() {
        return periodCode;
    }

    public void setPeriodCode(String periodCode) {
        this.periodCode = periodCode;
    }

    public BasPeriodType getPeriodType() {
        return periodType;
    }

    public void setPeriodType(BasPeriodType periodType) {
        this.periodType = periodType;
    }

    public LocalDate getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(LocalDate dateFrom) {
        this.dateFrom = dateFrom;
    }

    public LocalDate getDateTo() {
        return dateTo;
    }

    public void setDateTo(LocalDate dateTo) {
        this.dateTo = dateTo;
    }

    public BasPeriodStatus getStatus() {
        return status;
    }

    public void setStatus(BasPeriodStatus status) {
        this.status = status;
    }

    public Instant getLodgedAt() {
        return lodgedAt;
    }

    public void setLodgedAt(Instant lodgedAt) {
        this.lodgedAt = lodgedAt;
    }

    public String getLodgedBy() {
        return lodgedBy;
    }

    public void setLodgedBy(String lodgedBy) {
        this.lodgedBy = lodgedBy;
    }

    public String getLodgementReference() {
        return lodgementReference;
    }

    public void setLodgementReference(String lodgementReference) {
        this.lodgementReference = lodgementReference;
    }

    public String getGstAccountId() {
        return gstAccountId;
    }

    public void setGstAccountId(String gstAccountId) {
        this.gstAccountId = gstAccountId;
    }

    public String getRevenueAccountId() {
        return revenueAccountId;
    }

    public void setRevenueAccountId(String revenueAccountId) {
        this.revenueAccountId = revenueAccountId;
    }
}

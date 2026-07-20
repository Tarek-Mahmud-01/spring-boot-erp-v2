package com.springboot.erp.modules.finance.periods.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-015 FiscalPeriod (reference {@code app.business_settings.models.FiscalPeriod}) — the
 * fiscal period being closed. Generated up-front for a company (e.g. 12 at a time) and driven
 * through the full period-end lifecycle by {@code PeriodTransitionService}: DRAFT -&gt; OPEN -&gt;
 * PREPARING -&gt; RECONCILING -&gt; VALIDATING -&gt; PENDING_APPROVAL -&gt; APPROVED -&gt; CLOSING -&gt; CLOSED
 * -&gt; (ADJUSTMENT &lt;-&gt; CLOSED) -&gt; ARCHIVED, with an audited reopen (CLOSED -&gt; OPEN).
 *
 * <p>{@code companyId} is a loose cross-module ULID reference (settings.company) — no hard FK, per
 * the vertical-slice rule. {@link com.springboot.erp.modules.finance.periods.domain.PeriodChecklistItem},
 * {@link PeriodApprovalStep}, and {@link PeriodSnapshot} ARE same-slice children and keep real FKs
 * to this entity.
 */
@Entity
@Table(name = "fiscal_periods")
public class FiscalPeriod extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "company_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String companyId;

    /** e.g. "2026-07" — matches {@code JournalEntry.periodCode} for posting-gate resolution. */
    @Column(name = "period_code", nullable = false, length = 7)
    private String periodCode;

    @Column(name = "date_from", nullable = false)
    private LocalDate dateFrom;

    @Column(name = "date_to", nullable = false)
    private LocalDate dateTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private FiscalPeriodStatus status = FiscalPeriodStatus.OPEN;

    public FiscalPeriod() {
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

    public FiscalPeriodStatus getStatus() {
        return status;
    }

    public void setStatus(FiscalPeriodStatus status) {
        this.status = status;
    }
}

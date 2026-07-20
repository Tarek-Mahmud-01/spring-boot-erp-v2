package com.springboot.erp.modules.finance.periods.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * E-009 Phase 5 — an immutable financial snapshot frozen at period close (reference
 * {@code app.finance.models_period_close.PeriodSnapshot}). Created when a {@link FiscalPeriod}
 * transitions CLOSING -&gt; CLOSED; holds a JSON payload of the period's financials as-of the close
 * (trial balance today — P&amp;L / balance-sheet builders are a future addition, the payload shape
 * leaves room). Versioned ({@code versionNo}) so a reopen -&gt; re-close keeps every prior run on
 * record for audit — mirrors {@link BasReport}. This row is NEVER updated once written; only new
 * versions are appended. Read-only historical record.
 *
 * <p>Same-slice child of {@link FiscalPeriod} — real FK.
 */
@Entity
@Table(name = "period_snapshots")
public class PeriodSnapshot extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_period_id", nullable = false)
    private FiscalPeriod fiscalPeriod;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt = Instant.now();

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "generated_by", length = 26, columnDefinition = "char(26)")
    private String generatedBy;

    /** {"trialBalance": {...}} — all integer minor units. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload = Map.of();

    @Column(name = "version_no", nullable = false)
    private int versionNo = 1;

    public PeriodSnapshot() {
    }

    public FiscalPeriod getFiscalPeriod() {
        return fiscalPeriod;
    }

    public void setFiscalPeriod(FiscalPeriod fiscalPeriod) {
        this.fiscalPeriod = fiscalPeriod;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public int getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(int versionNo) {
        this.versionNo = versionNo;
    }
}

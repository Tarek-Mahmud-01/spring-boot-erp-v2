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
 * ENT-AU-002 BasReport (reference {@code app.finance.models_bas.BasReport}) — an IMMUTABLE snapshot
 * of one BAS box-computation run for a {@link BasPeriod} (FR-AU-020/021), versioned so regenerating
 * a period's BAS after new GL postings land keeps every prior run on record for audit (AC-12). This
 * row is never updated after insert; a regeneration always appends a new {@code versionNo}.
 *
 * <p>{@code boxValues} = {"G1": 12345, "G2": 0, "G3": 200, "G10": 0, "G11": 4260, "1A": 1140, "1B":
 * 364, "netGst": 776} — all integer minor units (reference box shape).
 *
 * <p>Same-slice child of {@link BasPeriod} — real FK.
 */
@Entity
@Table(name = "bas_reports")
public class BasReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bas_period_id", nullable = false)
    private BasPeriod basPeriod;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt = Instant.now();

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "generated_by", length = 26, columnDefinition = "char(26)")
    private String generatedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "box_values", nullable = false, columnDefinition = "jsonb")
    private Map<String, Long> boxValues = Map.of();

    @Column(name = "version_no", nullable = false)
    private int versionNo = 1;

    public BasReport() {
    }

    public BasPeriod getBasPeriod() {
        return basPeriod;
    }

    public void setBasPeriod(BasPeriod basPeriod) {
        this.basPeriod = basPeriod;
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

    public Map<String, Long> getBoxValues() {
        return boxValues;
    }

    public void setBoxValues(Map<String, Long> boxValues) {
        this.boxValues = boxValues;
    }

    public int getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(int versionNo) {
        this.versionNo = versionNo;
    }
}

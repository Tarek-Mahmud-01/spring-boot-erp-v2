package com.guru.erp.modules.inventory.counts.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-044 CycleCountPlan — a physical count plan and its counted lines
 * (US-025 / FR-134-138). Header + line aggregate: lines are owned via a
 * cascade {@link OneToMany}, mirroring the reference {@code selectin} +
 * {@code cascade="all, delete-orphan"} relationship.
 *
 * <p>Cross-slice references ({@code locationId}) are loose ULID
 * {@code char(26)} public ids. {@code scope}/{@code status} persist as
 * check-constrained strings (scope = uppercase constant name, status via
 * {@link CycleCountStatusConverter}). {@code accuracyPct} is NUMERIC(5,2)
 * computed at approval time = fraction of zero-variance lines.
 *
 * <p>Constraint / index parity in V32__inventory_counts.sql:
 * {@code unique(number)}, {@code index(location_id)},
 * plus the line constraints on {@link CycleCountLine}.
 */
@Entity
@Table(name = "cycle_count_plans")
public class CycleCountPlan extends BaseEntity {

    @Column(name = "number", nullable = false, unique = true, length = 30)
    private String number;

    /** ULID public id of the counted location (cross-slice, resolved app-side). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "location_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String locationId;

    // Persisted as the uppercase constant name (ALL/CATEGORY/ABC/MANUAL) matching
    // ck_cycle_count_plans_scope; the reference StrEnum values are already uppercase.
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 10)
    private CycleCountScope scope = CycleCountScope.ALL;

    /** Optional scope selector payload (category_ids / product_ids). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scope_config", columnDefinition = "jsonb")
    private Map<String, Object> scopeConfig;

    @Column(name = "planned_date", nullable = false)
    private Instant plannedDate;

    @Convert(converter = CycleCountStatusConverter.class)
    @Column(name = "status", nullable = false, length = 15)
    private CycleCountStatus status = CycleCountStatus.DRAFT;

    /** Count accuracy % (zero-variance lines / total), set at approval. */
    @Column(name = "accuracy_pct", precision = 5, scale = 2)
    private BigDecimal accuracyPct;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo asc")
    private List<CycleCountLine> lines = new ArrayList<>();

    public CycleCountPlan() {
    }

    /** Attach a line and set its back-reference (aggregate consistency). */
    public void addLine(CycleCountLine line) {
        line.setPlan(this);
        this.lines.add(line);
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public CycleCountScope getScope() {
        return scope;
    }

    public void setScope(CycleCountScope scope) {
        this.scope = scope;
    }

    public Map<String, Object> getScopeConfig() {
        return scopeConfig;
    }

    public void setScopeConfig(Map<String, Object> scopeConfig) {
        this.scopeConfig = scopeConfig;
    }

    public Instant getPlannedDate() {
        return plannedDate;
    }

    public void setPlannedDate(Instant plannedDate) {
        this.plannedDate = plannedDate;
    }

    public CycleCountStatus getStatus() {
        return status;
    }

    public void setStatus(CycleCountStatus status) {
        this.status = status;
    }

    public BigDecimal getAccuracyPct() {
        return accuracyPct;
    }

    public void setAccuracyPct(BigDecimal accuracyPct) {
        this.accuracyPct = accuracyPct;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<CycleCountLine> getLines() {
        return lines;
    }

    public void setLines(List<CycleCountLine> lines) {
        this.lines = lines;
    }
}

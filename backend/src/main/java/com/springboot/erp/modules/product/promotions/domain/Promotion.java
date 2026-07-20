package com.springboot.erp.modules.product.promotions.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-014 Promotion — US-014 / FR-065..069. A configurable discount rule with a
 * validity window, scope, and stackability/reason-required governance flags.
 *
 * <p>Domain columns only; id / publicId / audit / version / soft-delete come
 * from {@link BaseEntity}. The owning company is referenced loosely by its ULID
 * {@code companyId} (char(26)) — no hard cross-slice FK (app-layer resolution).
 *
 * <p>Constraints reproduced in V22__product_promotions.sql:
 * <ul>
 *   <li>{@code ck_promotions_type} / {@code ck_promotions_status} — enum values.</li>
 *   <li>{@code ck_promotions_date_range} — {@code date_to > date_from}.</li>
 * </ul>
 * {@code config} and {@code scope} are opaque JSON (jsonb). Name uniqueness per
 * company among live rows is enforced in the service (case-insensitive).
 */
@Entity
@Table(name = "promotions")
public class Promotion extends BaseEntity {

    /** ULID of the owning company (cross-slice reference — no hard FK). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "company_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String companyId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PromotionType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> config = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scope", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> scope = Map.of();

    /** Optional customer-segment ULID this promotion is limited to. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "segment_id", length = 26, columnDefinition = "char(26)")
    private String segmentId;

    @Column(name = "date_from", nullable = false)
    private Instant dateFrom;

    @Column(name = "date_to", nullable = false)
    private Instant dateTo;

    @Column(name = "stackable", nullable = false)
    private boolean stackable = false;

    @Column(name = "reason_required", nullable = false)
    private boolean reasonRequired = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PromotionStatus status = PromotionStatus.DRAFT;

    public Promotion() {
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PromotionType getType() {
        return type;
    }

    public void setType(PromotionType type) {
        this.type = type;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public Map<String, Object> getScope() {
        return scope;
    }

    public void setScope(Map<String, Object> scope) {
        this.scope = scope;
    }

    public String getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(String segmentId) {
        this.segmentId = segmentId;
    }

    public Instant getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(Instant dateFrom) {
        this.dateFrom = dateFrom;
    }

    public Instant getDateTo() {
        return dateTo;
    }

    public void setDateTo(Instant dateTo) {
        this.dateTo = dateTo;
    }

    public boolean isStackable() {
        return stackable;
    }

    public void setStackable(boolean stackable) {
        this.stackable = stackable;
    }

    public boolean isReasonRequired() {
        return reasonRequired;
    }

    public void setReasonRequired(boolean reasonRequired) {
        this.reasonRequired = reasonRequired;
    }

    public PromotionStatus getStatus() {
        return status;
    }

    public void setStatus(PromotionStatus status) {
        this.status = status;
    }
}

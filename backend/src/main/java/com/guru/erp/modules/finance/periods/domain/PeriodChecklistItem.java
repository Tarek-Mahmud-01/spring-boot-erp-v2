package com.guru.erp.modules.finance.periods.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * FR-249 — one period-close checklist item for a {@link FiscalPeriod} (reference
 * {@code app.finance.models_period_close.PeriodChecklistItem}). Every item ships {@code required =
 * true} (strict/"hard" close mode — there are no advisory items); a period cannot advance
 * VALIDATING -&gt; PENDING_APPROVAL until every item is both {@link ChecklistCheckStatus#PASSED} and
 * signed off (see {@code PeriodChecklistService#assertChecklistComplete}).
 *
 * <p>Same-slice child of {@link FiscalPeriod} — real FK, {@code RESTRICT} on delete (a fiscal period
 * is never hard-deleted once it has checklist history).
 */
@Entity
@Table(name = "period_checklist_items")
public class PeriodChecklistItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_period_id", nullable = false)
    private FiscalPeriod fiscalPeriod;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_key", nullable = false, length = 40)
    private ChecklistItemKey itemKey;

    /** Whether this item gates VALIDATING -&gt; PENDING_APPROVAL. Always true today (strict mode). */
    @Column(name = "required", nullable = false)
    private boolean required = true;

    /** Configurable owner (FR-249) — loose ULID ref to the responsible user (access.user). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "owner_user_id", length = 26, columnDefinition = "char(26)")
    private String ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_status", nullable = false, length = 16)
    private ChecklistCheckStatus checkStatus = ChecklistCheckStatus.PENDING;

    /** Snapshot of the check's supporting detail, e.g. {"count": 3} / {"variance": 500}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "check_detail", columnDefinition = "jsonb")
    private Map<String, Object> checkDetail;

    @Column(name = "checked_at")
    private Instant checkedAt;

    /** Manual owner sign-off — only allowed once {@code checkStatus == PASSED}. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "signed_off_by", length = 26, columnDefinition = "char(26)")
    private String signedOffBy;

    @Column(name = "signed_off_at")
    private Instant signedOffAt;

    public PeriodChecklistItem() {
    }

    public FiscalPeriod getFiscalPeriod() {
        return fiscalPeriod;
    }

    public void setFiscalPeriod(FiscalPeriod fiscalPeriod) {
        this.fiscalPeriod = fiscalPeriod;
    }

    public ChecklistItemKey getItemKey() {
        return itemKey;
    }

    public void setItemKey(ChecklistItemKey itemKey) {
        this.itemKey = itemKey;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public ChecklistCheckStatus getCheckStatus() {
        return checkStatus;
    }

    public void setCheckStatus(ChecklistCheckStatus checkStatus) {
        this.checkStatus = checkStatus;
    }

    public Map<String, Object> getCheckDetail() {
        return checkDetail;
    }

    public void setCheckDetail(Map<String, Object> checkDetail) {
        this.checkDetail = checkDetail;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }

    public String getSignedOffBy() {
        return signedOffBy;
    }

    public void setSignedOffBy(String signedOffBy) {
        this.signedOffBy = signedOffBy;
    }

    public Instant getSignedOffAt() {
        return signedOffAt;
    }

    public void setSignedOffAt(Instant signedOffAt) {
        this.signedOffAt = signedOffAt;
    }
}

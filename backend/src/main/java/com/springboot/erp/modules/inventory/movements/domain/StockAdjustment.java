package com.springboot.erp.modules.inventory.movements.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-043 StockAdjustment — corrects on-hand stock at a location with a reason code and an
 * approval workflow (DRAFT → PENDING_APPROVAL/APPROVED → POSTED → REVERSED). Header + lines
 * aggregate; posting emits ledger movements via an outbox event (see PostingService).
 *
 * <p>Domain columns only; base columns from {@link BaseEntity}. Cross-slice references (location,
 * variance GL account, journal entry, approver user) are held as loose ULID {@code char(26)}
 * columns — no hard cross-slice FK. Constraints reproduced in V31: status check + the
 * location/status/created_at composite index.
 */
@Entity
@Table(name = "stock_adjustments")
public class StockAdjustment extends BaseEntity {

    @Column(name = "number", nullable = false, unique = true, length = 30)
    private String number;

    /** ULID public id of the Location (cross-slice, resolved app-side). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "location_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String locationId;

    @Column(name = "reason", nullable = false, length = 200)
    private String reason;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Convert(converter = AdjustmentStatusConverter.class)
    @Column(name = "status", nullable = false, length = 20)
    private AdjustmentStatus status = AdjustmentStatus.DRAFT;

    @Column(name = "threshold_exceeded", nullable = false)
    private boolean thresholdExceeded = false;

    /** ULID public id of the approving user; null until approved (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "approver_id", length = 26, columnDefinition = "char(26)")
    private String approverId;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "posted_at")
    private Instant postedAt;

    /** ULID public id of the GL variance account; required at POST (cross-slice, loose ref). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "variance_account_id", length = 26, columnDefinition = "char(26)")
    private String varianceAccountId;

    /** ULID public id of the JournalEntry created on POST; null before posting (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "journal_entry_id", length = 26, columnDefinition = "char(26)")
    private String journalEntryId;

    @OneToMany(mappedBy = "adjustment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo asc")
    private List<StockAdjustmentLine> lines = new ArrayList<>();

    public StockAdjustment() {
    }

    public void addLine(StockAdjustmentLine line) {
        line.setAdjustment(this);
        lines.add(line);
    }

    public void clearLines() {
        lines.clear();
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public AdjustmentStatus getStatus() {
        return status;
    }

    public void setStatus(AdjustmentStatus status) {
        this.status = status;
    }

    public boolean isThresholdExceeded() {
        return thresholdExceeded;
    }

    public void setThresholdExceeded(boolean thresholdExceeded) {
        this.thresholdExceeded = thresholdExceeded;
    }

    public String getApproverId() {
        return approverId;
    }

    public void setApproverId(String approverId) {
        this.approverId = approverId;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(Instant postedAt) {
        this.postedAt = postedAt;
    }

    public String getVarianceAccountId() {
        return varianceAccountId;
    }

    public void setVarianceAccountId(String varianceAccountId) {
        this.varianceAccountId = varianceAccountId;
    }

    public String getJournalEntryId() {
        return journalEntryId;
    }

    public void setJournalEntryId(String journalEntryId) {
        this.journalEntryId = journalEntryId;
    }

    public List<StockAdjustmentLine> getLines() {
        return lines;
    }
}

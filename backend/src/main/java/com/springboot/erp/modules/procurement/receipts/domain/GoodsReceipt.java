package com.springboot.erp.modules.procurement.receipts.domain;

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
 * ENT-029 GoodsReceipt (US-018 / FR-094–097) — a receipt of goods against a Purchase Order. Header
 * + lines aggregate; confirming a receipt ("receive") posts stock movements via an outbox event
 * (see {@code ReceiptPostingService}) and rolls received quantities up to the PO — this slice never
 * hard-calls the inventory or procurement PO slices.
 *
 * <p>Domain columns only; base columns from {@link BaseEntity}. Cross-slice references (PO,
 * location, receiver user) are held as loose ULID {@code char(26)} columns — no hard cross-slice
 * FK. The {@code po_id} carries the PurchaseOrder's public id (the reference used an internal FK;
 * here it is a loose ref per the vertical-slice rule). Constraints reproduced in V42: unique
 * {@code number}, status check, and the {@code received_at} index.
 */
@Entity
@Table(name = "goods_receipts")
public class GoodsReceipt extends BaseEntity {

    @Column(name = "number", nullable = false, unique = true, length = 30)
    private String number;

    /** ULID public id of the source PurchaseOrder (cross-slice, resolved app-side). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "po_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String poId;

    /** ULID public id of the receiving Location (cross-slice, resolved app-side). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "location_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String locationId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    /** ULID public id of the receiving user; null if unknown (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "received_by", length = 26, columnDefinition = "char(26)")
    private String receivedBy;

    @Convert(converter = GrnStatusConverter.class)
    @Column(name = "status", nullable = false, length = 20)
    private GrnStatus status = GrnStatus.DRAFT;

    @Column(name = "auto_receipt", nullable = false)
    private boolean autoReceipt = false;

    @Column(name = "delivery_note_no", length = 100)
    private String deliveryNoteNo;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @OneToMany(mappedBy = "grn", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<GoodsReceiptLine> lines = new ArrayList<>();

    public GoodsReceipt() {
    }

    public void addLine(GoodsReceiptLine line) {
        line.setGrn(this);
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

    public String getPoId() {
        return poId;
    }

    public void setPoId(String poId) {
        this.poId = poId;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getReceivedBy() {
        return receivedBy;
    }

    public void setReceivedBy(String receivedBy) {
        this.receivedBy = receivedBy;
    }

    public GrnStatus getStatus() {
        return status;
    }

    public void setStatus(GrnStatus status) {
        this.status = status;
    }

    public boolean isAutoReceipt() {
        return autoReceipt;
    }

    public void setAutoReceipt(boolean autoReceipt) {
        this.autoReceipt = autoReceipt;
    }

    public String getDeliveryNoteNo() {
        return deliveryNoteNo;
    }

    public void setDeliveryNoteNo(String deliveryNoteNo) {
        this.deliveryNoteNo = deliveryNoteNo;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<GoodsReceiptLine> getLines() {
        return lines;
    }
}

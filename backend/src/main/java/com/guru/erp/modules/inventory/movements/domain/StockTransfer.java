package com.guru.erp.modules.inventory.movements.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-042 StockTransfer — moves stock between two locations with a status workflow
 * (DRAFT → APPROVED → PARTIALLY_COMPLETE/COMPLETE). Header + lines aggregate; the lines cascade.
 *
 * <p>Domain columns only; id/publicId/audit/version/soft-delete come from {@link BaseEntity}.
 * Cross-slice references (source/destination location) are held as loose ULID {@code char(26)}
 * columns per the vertical-slice rule — no hard cross-slice FK. Constraints reproduced in
 * V31__inventory_movements.sql: {@code ck_transfers_different_locations}, the status check, and
 * the status+created_at composite index.
 */
@Entity
@Table(name = "stock_transfers")
public class StockTransfer extends BaseEntity {

    @Column(name = "number", nullable = false, unique = true, length = 30)
    private String number;

    /** ULID public id of the source Location (cross-slice, resolved app-side). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "source_location_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String sourceLocationId;

    /** ULID public id of the destination Location (cross-slice, resolved app-side). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "destination_location_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String destinationLocationId;

    @Convert(converter = TransferStatusConverter.class)
    @Column(name = "status", nullable = false, length = 15)
    private TransferStatus status = TransferStatus.DRAFT;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    /** Business date the physical stock moved; never in the future (validated in the service). */
    @Column(name = "transfer_date")
    private LocalDate transferDate;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "received_at")
    private Instant receivedAt;

    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo asc")
    private List<StockTransferLine> lines = new ArrayList<>();

    public StockTransfer() {
    }

    public void addLine(StockTransferLine line) {
        line.setTransfer(this);
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

    public String getSourceLocationId() {
        return sourceLocationId;
    }

    public void setSourceLocationId(String sourceLocationId) {
        this.sourceLocationId = sourceLocationId;
    }

    public String getDestinationLocationId() {
        return destinationLocationId;
    }

    public void setDestinationLocationId(String destinationLocationId) {
        this.destinationLocationId = destinationLocationId;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public void setStatus(TransferStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDate getTransferDate() {
        return transferDate;
    }

    public void setTransferDate(LocalDate transferDate) {
        this.transferDate = transferDate;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public List<StockTransferLine> getLines() {
        return lines;
    }
}

package com.springboot.erp.modules.procurement.orders.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-028b PurchaseOrderVersion — FR-092 amendment history. One immutable snapshot row per PO
 * amendment (and one on create). {@code snapshot} is the JSON header snapshot captured at that
 * revision; {@code versionNo} mirrors the parent {@link PurchaseOrder#getPoVersion()} at capture.
 *
 * <p>{@code purchaseOrder} is a real same-slice FK. Extends {@link BaseEntity} for the standard
 * base columns (the reference kept only created_by/at; here every table carries the full base set).
 */
@Entity
@Table(name = "purchase_order_versions")
public class PurchaseOrderVersion extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> snapshot;

    @Column(name = "reason", length = 500)
    private String reason;

    public PurchaseOrderVersion() {
    }

    public PurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public void setPurchaseOrder(PurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
    }

    public int getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(int versionNo) {
        this.versionNo = versionNo;
    }

    public Map<String, Object> getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(Map<String, Object> snapshot) {
        this.snapshot = snapshot;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

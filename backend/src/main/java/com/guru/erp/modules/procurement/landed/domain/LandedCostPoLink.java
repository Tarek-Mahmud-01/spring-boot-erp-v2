package com.guru.erp.modules.procurement.landed.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-032c LandedCostPoLink — many-to-many landed cost ↔ PurchaseOrder (reference
 * {@code app.procurement.models.LandedCostPoLink}). One charge (a freight invoice for a shared
 * container) may cover several POs; this bridge captures the full PO set, beyond the single
 * "primary" {@code LandedCost.poId} shown on the header. Both sides are loose ULID {@code char(26)}
 * cross-slice refs. Reproduces {@code uq_landed_cost_po_links (landed_cost_id, po_id)}.
 */
@Entity
@Table(name = "landed_cost_po_links")
public class LandedCostPoLink extends BaseEntity {

    /** ULID public id of the owning LandedCost (same-slice, held as a ULID for the bridge shape). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "landed_cost_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String landedCostId;

    /** ULID public id of the linked PurchaseOrder (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "po_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String poId;

    public LandedCostPoLink() {
    }

    public LandedCostPoLink(String landedCostId, String poId) {
        this.landedCostId = landedCostId;
        this.poId = poId;
    }

    public String getLandedCostId() {
        return landedCostId;
    }

    public void setLandedCostId(String landedCostId) {
        this.landedCostId = landedCostId;
    }

    public String getPoId() {
        return poId;
    }

    public void setPoId(String poId) {
        this.poId = poId;
    }
}

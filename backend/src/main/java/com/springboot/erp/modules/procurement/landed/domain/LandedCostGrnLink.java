package com.springboot.erp.modules.procurement.landed.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-032b LandedCostGrnLink — many-to-many landed cost ↔ GoodsReceipt (reference
 * {@code app.procurement.models.LandedCostGrnLink}). A single freight bill can cover several inbound
 * shipments; this bridge captures the full GRN set the cost applies to, beyond the single "primary"
 * {@code LandedCost.grnId} shown on the header. Both sides are loose ULID {@code char(26)}
 * cross-slice refs. Reproduces {@code uq_landed_cost_grn_links (landed_cost_id, grn_id)}.
 */
@Entity
@Table(name = "landed_cost_grn_links")
public class LandedCostGrnLink extends BaseEntity {

    /** ULID public id of the owning LandedCost (same-slice, held as a ULID for the bridge shape). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "landed_cost_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String landedCostId;

    /** ULID public id of the linked GoodsReceipt (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "grn_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String grnId;

    public LandedCostGrnLink() {
    }

    public LandedCostGrnLink(String landedCostId, String grnId) {
        this.landedCostId = landedCostId;
        this.grnId = grnId;
    }

    public String getLandedCostId() {
        return landedCostId;
    }

    public void setLandedCostId(String landedCostId) {
        this.landedCostId = landedCostId;
    }

    public String getGrnId() {
        return grnId;
    }

    public void setGrnId(String grnId) {
        this.grnId = grnId;
    }
}

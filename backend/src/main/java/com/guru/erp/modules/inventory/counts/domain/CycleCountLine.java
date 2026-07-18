package com.guru.erp.modules.inventory.counts.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * ENT-044a CycleCountLine — one counted line inside a {@link CycleCountPlan}.
 *
 * <p>Quantities are {@link BigDecimal} (NUMERIC(18,6)) so fractional (weighed)
 * goods count exactly — never {@code double}. {@code productId} is a loose ULID
 * cross-slice reference (no hard FK to the product catalog per the vertical-slice
 * rule). Variance = counted - expected; {@code requiresRecount} flags a non-zero
 * first-pass variance that a second pass must reconcile.
 *
 * <p>Constraints reproduced in V32__inventory_counts.sql:
 * {@code uq_cycle_count_lines_plan_line(plan_id, line_no)} and
 * {@code ck_cycle_count_lines_qty_expected_non_negative(qty_expected >= 0)}.
 */
@Entity
@Table(name = "cycle_count_lines")
public class CycleCountLine extends BaseEntity {

    @ManyToOne(optional = false, fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private CycleCountPlan plan;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    /** ULID public id of the counted product (cross-slice, resolved app-side). */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
    @Column(name = "product_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String productId;

    @Column(name = "qty_expected", nullable = false, precision = 18, scale = 6)
    private BigDecimal qtyExpected = BigDecimal.ZERO;

    @Column(name = "qty_first_pass", precision = 18, scale = 6)
    private BigDecimal qtyFirstPass;

    @Column(name = "qty_second_pass", precision = 18, scale = 6)
    private BigDecimal qtySecondPass;

    @Column(name = "variance", nullable = false, precision = 18, scale = 6)
    private BigDecimal variance = BigDecimal.ZERO;

    @Column(name = "requires_recount", nullable = false)
    private boolean requiresRecount = false;

    public CycleCountLine() {
    }

    public CycleCountPlan getPlan() {
        return plan;
    }

    public void setPlan(CycleCountPlan plan) {
        this.plan = plan;
    }

    public int getLineNo() {
        return lineNo;
    }

    public void setLineNo(int lineNo) {
        this.lineNo = lineNo;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public BigDecimal getQtyExpected() {
        return qtyExpected;
    }

    public void setQtyExpected(BigDecimal qtyExpected) {
        this.qtyExpected = qtyExpected;
    }

    public BigDecimal getQtyFirstPass() {
        return qtyFirstPass;
    }

    public void setQtyFirstPass(BigDecimal qtyFirstPass) {
        this.qtyFirstPass = qtyFirstPass;
    }

    public BigDecimal getQtySecondPass() {
        return qtySecondPass;
    }

    public void setQtySecondPass(BigDecimal qtySecondPass) {
        this.qtySecondPass = qtySecondPass;
    }

    public BigDecimal getVariance() {
        return variance;
    }

    public void setVariance(BigDecimal variance) {
        this.variance = variance;
    }

    public boolean isRequiresRecount() {
        return requiresRecount;
    }

    public void setRequiresRecount(boolean requiresRecount) {
        this.requiresRecount = requiresRecount;
    }
}

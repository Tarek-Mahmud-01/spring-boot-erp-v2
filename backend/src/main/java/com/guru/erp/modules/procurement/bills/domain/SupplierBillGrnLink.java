package com.guru.erp.modules.procurement.bills.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-030b SupplierBillGrnLink — many-to-many bill ↔ GoodsReceipt. A bill can cover several GRNs;
 * each row links one. {@code bill} is a real same-slice FK; {@code grnId} is a loose cross-slice
 * ULID ref to the GoodsReceipt. Uniqueness {@code uq_bill_grn_links(bill_id, grn_id)} enforced in V43.
 */
@Entity
@Table(name = "supplier_bill_grn_links")
public class SupplierBillGrnLink extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "bill_id", nullable = false)
    private SupplierBill bill;

    /** ULID public id of the linked GoodsReceipt (cross-slice, resolved app-side). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "grn_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String grnId;

    public SupplierBillGrnLink() {
    }

    public SupplierBillGrnLink(String grnId) {
        this.grnId = grnId;
    }

    public SupplierBill getBill() {
        return bill;
    }

    public void setBill(SupplierBill bill) {
        this.bill = bill;
    }

    public String getGrnId() {
        return grnId;
    }

    public void setGrnId(String grnId) {
        this.grnId = grnId;
    }
}

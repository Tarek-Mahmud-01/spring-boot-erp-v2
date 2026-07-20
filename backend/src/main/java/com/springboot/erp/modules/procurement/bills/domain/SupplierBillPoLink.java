package com.springboot.erp.modules.procurement.bills.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-030c SupplierBillPoLink — many-to-many bill ↔ PurchaseOrder. The header carries a "primary"
 * {@code poId}, but bill lines can reference different POs, so a bill can span multiple POs; this
 * link makes the full set queryable. {@code bill} is a real same-slice FK; {@code poId} is a loose
 * cross-slice ULID ref. Uniqueness {@code uq_bill_po_links(bill_id, po_id)} is enforced in V43.
 */
@Entity
@Table(name = "supplier_bill_po_links")
public class SupplierBillPoLink extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "bill_id", nullable = false)
    private SupplierBill bill;

    /** ULID public id of the linked PurchaseOrder (cross-slice, resolved app-side). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "po_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String poId;

    public SupplierBillPoLink() {
    }

    public SupplierBillPoLink(String poId) {
        this.poId = poId;
    }

    public SupplierBill getBill() {
        return bill;
    }

    public void setBill(SupplierBill bill) {
        this.bill = bill;
    }

    public String getPoId() {
        return poId;
    }

    public void setPoId(String poId) {
        this.poId = poId;
    }
}

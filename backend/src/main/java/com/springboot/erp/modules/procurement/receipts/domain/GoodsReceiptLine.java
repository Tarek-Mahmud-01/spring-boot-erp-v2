package com.springboot.erp.modules.procurement.receipts.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import com.springboot.erp.platform.money.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-029a GoodsReceiptLine — one received product line of a {@link GoodsReceipt}. Quantities are
 * {@code numeric(18,6)} {@link BigDecimal} (reference precision — a weighed product is received as
 * 12.675 kg). MRP and sell price are optional {@link Money} pairs captured from the physical label
 * at receive time.
 *
 * <p>{@code grn} is a real same-slice FK; po_line / variant are loose cross-slice ULID refs.
 * Constraints reproduced in the migration: {@code ck_grn_lines_qty_non_negative},
 * {@code ck_grn_lines_discrepancy_non_negative}, and the po_line index.
 */
@Entity
@Table(name = "goods_receipt_lines")
public class GoodsReceiptLine extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "grn_id", nullable = false)
    private GoodsReceipt grn;

    /** ULID public id of the source PurchaseOrderLine; null for standalone (non-PO) lines. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "po_line_id", length = 26, columnDefinition = "char(26)")
    private String poLineId;

    /** ULID public id of the ProductVariant received; null for non-variant products (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "variant_id", length = 26, columnDefinition = "char(26)")
    private String variantId;

    @Column(name = "qty_received", nullable = false, precision = 18, scale = 6)
    private BigDecimal qtyReceived = BigDecimal.ZERO;

    @Column(name = "qty_discrepancy", nullable = false, precision = 18, scale = 6)
    private BigDecimal qtyDiscrepancy = BigDecimal.ZERO;

    @Convert(converter = DiscrepancyTypeConverter.class)
    @Column(name = "discrepancy_type", length = 15)
    private DiscrepancyType discrepancyType;

    @Column(name = "discrepancy_note", length = 500)
    private String discrepancyNote;

    @Column(name = "batch_no", length = 100)
    private String batchNo;

    @Column(name = "serial_no", length = 100)
    private String serialNo;

    @Column(name = "expiry_date")
    private Instant expiryDate;

    /** Barcode printed on the physical pack by the manufacturer/supplier; used at confirm time. */
    @Column(name = "supplier_barcode", length = 64)
    private String supplierBarcode;

    @Column(name = "manufacture_date")
    private Instant manufactureDate;

    /** Maximum retail price on the physical label (informational). Null → not captured. */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "mrp_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "mrp_currency", length = 3, columnDefinition = "char(3)"))
    })
    private Money mrp;

    /** Sell price for this batch; if set and differing, downstream creates a PriceHistory row. */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "sell_price_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "sell_price_currency", length = 3, columnDefinition = "char(3)"))
    })
    private Money sellPrice;

    public GoodsReceiptLine() {
    }

    /** Usable qty that hits stock: {@code qty_received - qty_discrepancy}, floored at zero. */
    public BigDecimal usableQty() {
        BigDecimal disc = qtyDiscrepancy == null ? BigDecimal.ZERO : qtyDiscrepancy;
        BigDecimal usable = (qtyReceived == null ? BigDecimal.ZERO : qtyReceived).subtract(disc);
        return usable.signum() > 0 ? usable : BigDecimal.ZERO;
    }

    public GoodsReceipt getGrn() {
        return grn;
    }

    public void setGrn(GoodsReceipt grn) {
        this.grn = grn;
    }

    public String getPoLineId() {
        return poLineId;
    }

    public void setPoLineId(String poLineId) {
        this.poLineId = poLineId;
    }

    public String getVariantId() {
        return variantId;
    }

    public void setVariantId(String variantId) {
        this.variantId = variantId;
    }

    public BigDecimal getQtyReceived() {
        return qtyReceived;
    }

    public void setQtyReceived(BigDecimal qtyReceived) {
        this.qtyReceived = qtyReceived;
    }

    public BigDecimal getQtyDiscrepancy() {
        return qtyDiscrepancy;
    }

    public void setQtyDiscrepancy(BigDecimal qtyDiscrepancy) {
        this.qtyDiscrepancy = qtyDiscrepancy;
    }

    public DiscrepancyType getDiscrepancyType() {
        return discrepancyType;
    }

    public void setDiscrepancyType(DiscrepancyType discrepancyType) {
        this.discrepancyType = discrepancyType;
    }

    public String getDiscrepancyNote() {
        return discrepancyNote;
    }

    public void setDiscrepancyNote(String discrepancyNote) {
        this.discrepancyNote = discrepancyNote;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getSupplierBarcode() {
        return supplierBarcode;
    }

    public void setSupplierBarcode(String supplierBarcode) {
        this.supplierBarcode = supplierBarcode;
    }

    public Instant getManufactureDate() {
        return manufactureDate;
    }

    public void setManufactureDate(Instant manufactureDate) {
        this.manufactureDate = manufactureDate;
    }

    public Money getMrp() {
        return mrp;
    }

    public void setMrp(Money mrp) {
        this.mrp = mrp;
    }

    public Money getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(Money sellPrice) {
        this.sellPrice = sellPrice;
    }
}

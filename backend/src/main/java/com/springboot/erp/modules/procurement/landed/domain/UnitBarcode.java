package com.springboot.erp.modules.procurement.landed.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Per-unit barcode assigned from a GRN line (reference {@code app.procurement.models.UnitBarcode}).
 * One row = one physical unit (or pack when {@code qty > 1}). {@code isBundle=true} means the
 * barcode covers multiple products, held in {@link UnitBarcodeItem}; otherwise the single
 * product / variant / qty on this header applies.
 *
 * <p>Prices are stored as bigint minor units + ISO-4217 currency, nullable, so a
 * {@code mrpAmount}/{@code mrpCurrency} and {@code sellPriceAmount}/{@code sellPriceCurrency} pair
 * is modelled as loose columns rather than a mandatory {@code Money} embeddable. The GRN line,
 * GRN, location, product and variant are loose ULID {@code char(26)} cross-slice refs.
 *
 * <p>Constraints reproduced in V45: {@code ck_unit_barcodes_format}, {@code ck_unit_barcodes_status},
 * {@code ck_unit_barcodes_qty_positive}, the unique {@code barcode}, plus the lookup indexes.
 */
@Entity
@Table(name = "unit_barcodes")
public class UnitBarcode extends BaseEntity {

    /** ULID public id of the source GoodsReceiptLine (cross-slice); null for a standalone bundle. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "grn_line_id", length = 26, columnDefinition = "char(26)")
    private String grnLineId;

    /** ULID public id of the source GoodsReceipt (cross-slice); null for a standalone bundle. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "grn_id", length = 26, columnDefinition = "char(26)")
    private String grnId;

    /** ULID public id of the Location (cross-slice); optional. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "location_id", length = 26, columnDefinition = "char(26)")
    private String locationId;

    @Column(name = "barcode", nullable = false, unique = true, length = 100)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(name = "barcode_format", nullable = false, length = 16)
    private BarcodeFormat barcodeFormat = BarcodeFormat.EAN13;

    @Column(name = "is_bundle", nullable = false)
    private boolean bundle = false;

    /** ULID public id of the Product (cross-slice); null when {@code isBundle}. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "product_id", length = 26, columnDefinition = "char(26)")
    private String productId;

    /** ULID public id of the ProductVariant (cross-slice); null when {@code isBundle} or no variant. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "variant_id", length = 26, columnDefinition = "char(26)")
    private String variantId;

    @Column(name = "qty", nullable = false, precision = 18, scale = 6)
    private BigDecimal qty = BigDecimal.ONE;

    @Column(name = "mrp_amount")
    private Long mrpAmount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "mrp_currency", length = 3, columnDefinition = "char(3)")
    private String mrpCurrency;

    @Column(name = "sell_price_amount")
    private Long sellPriceAmount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "sell_price_currency", length = 3, columnDefinition = "char(3)")
    private String sellPriceCurrency;

    @Column(name = "batch_no", length = 100)
    private String batchNo;

    @Column(name = "serial_no", length = 100)
    private String serialNo;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "entry_date")
    private LocalDate entryDate;

    @Convert(converter = UnitBarcodeStatusConverter.class)
    @Column(name = "status", nullable = false, length = 16)
    private UnitBarcodeStatus status = UnitBarcodeStatus.AVAILABLE;

    @Column(name = "notes", length = 500)
    private String notes;

    @OneToMany(mappedBy = "unitBarcode", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<UnitBarcodeItem> items = new ArrayList<>();

    public UnitBarcode() {
    }

    public void addItem(UnitBarcodeItem item) {
        item.setUnitBarcode(this);
        items.add(item);
    }

    public void clearItems() {
        items.clear();
    }

    public String getGrnLineId() {
        return grnLineId;
    }

    public void setGrnLineId(String grnLineId) {
        this.grnLineId = grnLineId;
    }

    public String getGrnId() {
        return grnId;
    }

    public void setGrnId(String grnId) {
        this.grnId = grnId;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public BarcodeFormat getBarcodeFormat() {
        return barcodeFormat;
    }

    public void setBarcodeFormat(BarcodeFormat barcodeFormat) {
        this.barcodeFormat = barcodeFormat;
    }

    public boolean isBundle() {
        return bundle;
    }

    public void setBundle(boolean bundle) {
        this.bundle = bundle;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getVariantId() {
        return variantId;
    }

    public void setVariantId(String variantId) {
        this.variantId = variantId;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public Long getMrpAmount() {
        return mrpAmount;
    }

    public void setMrpAmount(Long mrpAmount) {
        this.mrpAmount = mrpAmount;
    }

    public String getMrpCurrency() {
        return mrpCurrency;
    }

    public void setMrpCurrency(String mrpCurrency) {
        this.mrpCurrency = mrpCurrency;
    }

    public Long getSellPriceAmount() {
        return sellPriceAmount;
    }

    public void setSellPriceAmount(Long sellPriceAmount) {
        this.sellPriceAmount = sellPriceAmount;
    }

    public String getSellPriceCurrency() {
        return sellPriceCurrency;
    }

    public void setSellPriceCurrency(String sellPriceCurrency) {
        this.sellPriceCurrency = sellPriceCurrency;
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

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public UnitBarcodeStatus getStatus() {
        return status;
    }

    public void setStatus(UnitBarcodeStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<UnitBarcodeItem> getItems() {
        return items;
    }
}

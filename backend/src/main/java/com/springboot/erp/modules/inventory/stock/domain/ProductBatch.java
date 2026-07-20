package com.springboot.erp.modules.inventory.stock.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import com.springboot.erp.platform.money.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-045 ProductBatch — a batch/lot received on a source document (reference
 * {@code ProductBatch}), carrying its own barcode, MRP and sell price plus
 * manufacture / expiry dates for lot tracking. Resolves at POS scan time to give
 * the cashier the correct batch sell price. Feeds the {@code batch_id} on
 * {@link StockLedger} receipt / sale rows.
 *
 * <p>Cross-slice references (product, variant) are held loosely as ULID
 * {@code char(26)}. {@code grnCost} is required {@link Money}; MRP and sell price
 * are independently nullable so their amount + currency are plain nullable
 * columns rather than an embedded {@link Money} (which mandates a currency).
 * {@code qtyReceived} is {@code numeric(18,6)} to mirror the fractional GRN line
 * quantity (never {@code double}).
 *
 * <p>Constraints reproduced in V30__inventory_stock.sql:
 * <ul>
 *   <li>{@code uq_product_batches_barcode} — partial unique on barcode where not null.</li>
 *   <li>{@code ck_product_batches_barcode_format} / {@code ck_product_batches_source}.</li>
 *   <li>Indexes on (source_doc_type, source_doc_id) and expiry (partial, not null).</li>
 * </ul>
 */
@Entity
@Table(name = "product_batches")
public class ProductBatch extends BaseEntity {

    /** ULID public id of the product this batch is for (cross-slice, loose ref). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "product_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String productId;

    /** ULID public id of the variant; null for non-variant products. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "variant_id", length = 26, columnDefinition = "char(26)")
    private String variantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_doc_type", nullable = false, length = 20)
    private SourceDocType sourceDocType;

    @Column(name = "source_doc_id", nullable = false, length = 50)
    private String sourceDocId;

    @Column(name = "batch_no", length = 100)
    private String batchNo;

    /** Null for WEIGHTED barcodes (scale prints dynamically at weighing time). */
    @Column(name = "barcode", length = 64)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(name = "barcode_format", nullable = false, length = 16)
    private BatchBarcodeFormat barcodeFormat;

    @Convert(converter = BatchBarcodeSourceConverter.class)
    @Column(name = "barcode_source", nullable = false, length = 16)
    private BatchBarcodeSource barcodeSource = BatchBarcodeSource.MANUFACTURER;

    /** How many stock units one scan of this barcode represents. {@code numeric(18,6)}. */
    @Column(name = "qty_per_scan", nullable = false, precision = 18, scale = 6)
    private BigDecimal qtyPerScan = BigDecimal.ONE;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "grn_cost_amount", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "grn_cost_currency", nullable = false, length = 3, columnDefinition = "char(3)"))
    })
    private Money grnCost;

    @Column(name = "mrp_amount")
    private Long mrpAmount;

    @Column(name = "mrp_currency", length = 3, columnDefinition = "char(3)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String mrpCurrency;

    @Column(name = "sell_price_amount")
    private Long sellPriceAmount;

    @Column(name = "sell_price_currency", length = 3, columnDefinition = "char(3)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String sellPriceCurrency;

    /** Quantity received into this batch, {@code numeric(18,6)} (fractional units). */
    @Column(name = "qty_received", nullable = false, precision = 18, scale = 6)
    private BigDecimal qtyReceived = BigDecimal.ZERO;

    @Column(name = "manufacture_date")
    private Instant manufactureDate;

    @Column(name = "expiry_date")
    private Instant expiryDate;

    public ProductBatch() {
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

    public SourceDocType getSourceDocType() {
        return sourceDocType;
    }

    public void setSourceDocType(SourceDocType sourceDocType) {
        this.sourceDocType = sourceDocType;
    }

    public String getSourceDocId() {
        return sourceDocId;
    }

    public void setSourceDocId(String sourceDocId) {
        this.sourceDocId = sourceDocId;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public BatchBarcodeFormat getBarcodeFormat() {
        return barcodeFormat;
    }

    public void setBarcodeFormat(BatchBarcodeFormat barcodeFormat) {
        this.barcodeFormat = barcodeFormat;
    }

    public BatchBarcodeSource getBarcodeSource() {
        return barcodeSource;
    }

    public void setBarcodeSource(BatchBarcodeSource barcodeSource) {
        this.barcodeSource = barcodeSource;
    }

    public BigDecimal getQtyPerScan() {
        return qtyPerScan;
    }

    public void setQtyPerScan(BigDecimal qtyPerScan) {
        this.qtyPerScan = qtyPerScan;
    }

    public Money getGrnCost() {
        return grnCost;
    }

    public void setGrnCost(Money grnCost) {
        this.grnCost = grnCost;
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

    public BigDecimal getQtyReceived() {
        return qtyReceived;
    }

    public void setQtyReceived(BigDecimal qtyReceived) {
        this.qtyReceived = qtyReceived;
    }

    public Instant getManufactureDate() {
        return manufactureDate;
    }

    public void setManufactureDate(Instant manufactureDate) {
        this.manufactureDate = manufactureDate;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }
}

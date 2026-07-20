package com.springboot.erp.modules.crm.loyalty.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Read projection of a POS / Sales transaction, feeding purchase history and
 * loyalty/segment logic (reference {@code CustomerTransaction}, US-041). In
 * production POS / Sales feed this projection over the outbox
 * ({@code pos.sale.completed} / {@code pos.refund.created}); this slice owns
 * the projection table and its idempotent writer
 * ({@link com.springboot.erp.modules.crm.loyalty.service.CustomerTransactionService})
 * so history / spend-based loyalty logic are testable end-to-end even before a
 * dedicated outbox-subscriber dispatcher is wired in. Payment data is stored
 * already masked (last 4 digits only) — never the full PAN.
 *
 * <p>{@code customerId} is a loose ULID cross-slice ref (CRM customer, owned by
 * a different sub-slice). {@code lines} / {@code paymentSummary} are opaque
 * JSON snapshots — see the reference {@code TransactionLine} / {@code TransactionPayment}
 * shapes.
 *
 * <p>Constraints reproduced in V61__crm_loyalty.sql:
 * <ul>
 *   <li>{@code ck_customer_transactions_type} — type in (SALE, REFUND).</li>
 *   <li>{@code uq_customer_transactions_receipt} — one projection row per
 *       (customer, receipt_number, type), making the outbox-fed writer
 *       idempotent under at-least-once redelivery.</li>
 *   <li>{@code ix_customer_transactions_customer_occurred} — (customer_id, occurred_at).</li>
 * </ul>
 */
@Entity
@Table(name = "customer_transactions")
public class CustomerTransaction extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "customer_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String customerId;

    @Column(name = "receipt_number", nullable = false, length = 40)
    private String receiptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 8)
    private TransactionType type = TransactionType.SALE;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount = 0;

    /** Goods-only, tax-exclusive subtotal; null on rows projected before this column existed. */
    @Column(name = "subtotal_amount")
    private Long subtotalAmount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "total_currency", nullable = false, length = 3, columnDefinition = "char(3)")
    private String totalCurrency;

    /** Loose ULID cross-slice ref (settings.location). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "location_id", length = 26, columnDefinition = "char(26)")
    private String locationId;

    /** Already masked at write time: {@code [{"method","maskedPan","amount"}]}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payment_summary", nullable = false, columnDefinition = "jsonb")
    private List<Map<String, Object>> paymentSummary = List.of();

    /** {@code [{"sku","name","qty","unitPriceAmount","lineAmount","categoryId"}]}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "lines", nullable = false, columnDefinition = "jsonb")
    private List<Map<String, Object>> lines = List.of();

    /** ULID of the original sale this REFUND row reverses; null for SALE rows. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "refund_of_id", length = 26, columnDefinition = "char(26)")
    private String refundOfId;

    public CustomerTransaction() {
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Long getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(Long subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }

    public String getTotalCurrency() {
        return totalCurrency;
    }

    public void setTotalCurrency(String totalCurrency) {
        this.totalCurrency = totalCurrency;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public List<Map<String, Object>> getPaymentSummary() {
        return paymentSummary;
    }

    public void setPaymentSummary(List<Map<String, Object>> paymentSummary) {
        this.paymentSummary = paymentSummary;
    }

    public List<Map<String, Object>> getLines() {
        return lines;
    }

    public void setLines(List<Map<String, Object>> lines) {
        this.lines = lines;
    }

    public String getRefundOfId() {
        return refundOfId;
    }

    public void setRefundOfId(String refundOfId) {
        this.refundOfId = refundOfId;
    }
}

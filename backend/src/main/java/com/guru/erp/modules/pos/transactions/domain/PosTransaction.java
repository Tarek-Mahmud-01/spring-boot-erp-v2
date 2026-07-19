package com.guru.erp.modules.pos.transactions.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-PosTransaction — a POS sale or refund (reference {@code app.pos.models.PosTransaction}).
 * Lives OPEN as a cart (lines/tenders may still change), then COMPLETED on full payment, or
 * PARKED/VOIDED along the way. Header + lines + tenders aggregate.
 *
 * <p>Domain columns only; base columns from {@link BaseEntity}. Cross-slice references (register,
 * till session, location, cashier, customer, payment method) are held as loose ULID
 * {@code char(26)} columns — no hard cross-slice FK (this vertical slice never hard-calls the
 * register/till/product/customer slices); only the intra-aggregate line/tender relationships use
 * real FKs. Money columns are integer minor units sharing the header {@code currency} (never a
 * float), matching the reference. Constraints reproduced in V51: status/type checks, the unique
 * {@code client_txn_uuid}, and the status/receipt-number/register indexes.
 */
@Entity
@Table(name = "pos_transactions")
public class PosTransaction extends BaseEntity {

    /** ULID public id of the Register this sale rang on (cross-slice, required). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "register_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String registerId;

    /** ULID public id of the open PosTillSession, if any (cross-slice, loose ref). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "till_session_id", length = 26, columnDefinition = "char(26)")
    private String tillSessionId;

    /** ULID public id of the Location (cross-slice, required). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "location_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String locationId;

    /** ULID public id of the ringing cashier user; null for an unattributed offline replay. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cashier_id", length = 26, columnDefinition = "char(26)")
    private String cashierId;

    /** ULID public id of the attached Customer; null for a walk-in sale (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "customer_id", length = 26, columnDefinition = "char(26)")
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 8)
    private PosTransactionType type = PosTransactionType.SALE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private PosTransactionStatus status = PosTransactionStatus.OPEN;

    /** ULID public id of the original sale this REFUND reverses; null for a SALE (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "refund_of_id", length = 26, columnDefinition = "char(26)")
    private String refundOfId;

    @Column(name = "receipt_number", length = 40)
    private String receiptNumber;

    @Column(name = "document_type", length = 16)
    private String documentType;

    @Column(name = "subtotal_amount", nullable = false)
    private long subtotalAmount = 0L;

    @Column(name = "tax_amount", nullable = false)
    private long taxAmount = 0L;

    @Column(name = "discount_amount", nullable = false)
    private long discountAmount = 0L;

    /** Cashier/manager-applied order-level manual discount (subset of {@code discountAmount}). */
    @Column(name = "manual_discount_amount", nullable = false)
    private long manualDiscountAmount = 0L;

    @Column(name = "manual_discount_reason", length = 200)
    private String manualDiscountReason;

    /** ULID public id of the manager who approved a manual discount above threshold. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "manager_approval_by", length = 26, columnDefinition = "char(26)")
    private String managerApprovalBy;

    /** Snapshot of the approving manager's display name at approval time (never rewritten). */
    @Column(name = "manager_approval_name", length = 200)
    private String managerApprovalName;

    @Column(name = "manager_approval_at")
    private Instant managerApprovalAt;

    /** Payment-method surcharge net of GST, held on the header (never a product line). */
    @Column(name = "surcharge_amount", nullable = false)
    private long surchargeAmount = 0L;

    @Column(name = "surcharge_tax_amount", nullable = false)
    private long surchargeTaxAmount = 0L;

    @Column(name = "surcharge_taxable", nullable = false)
    private boolean surchargeTaxable = false;

    /** ULID public id of the payment method that triggered the surcharge (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "surcharge_method_id", length = 26, columnDefinition = "char(26)")
    private String surchargeMethodId;

    @Column(name = "surcharge_label", length = 100)
    private String surchargeLabel;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount = 0L;

    @Column(name = "paid_amount", nullable = false)
    private long paidAmount = 0L;

    @Column(name = "change_amount", nullable = false)
    private long changeAmount = 0L;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency = "USD";

    @Column(name = "age_verified", nullable = false)
    private boolean ageVerified = false;

    @Column(name = "age_verified_at")
    private Instant ageVerifiedAt;

    /** ULID public id of the cashier who performed the age check (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "age_verified_by", length = 26, columnDefinition = "char(26)")
    private String ageVerifiedBy;

    @Column(name = "age_id_type", length = 40)
    private String ageIdType;

    @Column(name = "reprint_count", nullable = false)
    private int reprintCount = 0;

    @Column(name = "offline_origin", nullable = false)
    private boolean offlineOrigin = false;

    /** Idempotency key for an offline-captured sale replayed on sync; null otherwise. */
    @Column(name = "client_txn_uuid", unique = true, length = 36)
    private String clientTxnUuid;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo asc")
    private List<PosTransactionLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence asc")
    private List<PosTender> tenders = new ArrayList<>();

    public PosTransaction() {
    }

    public void addLine(PosTransactionLine line) {
        line.setTransaction(this);
        lines.add(line);
    }

    public void addTender(PosTender tender) {
        tender.setTransaction(this);
        tenders.add(tender);
    }

    public String getRegisterId() {
        return registerId;
    }

    public void setRegisterId(String registerId) {
        this.registerId = registerId;
    }

    public String getTillSessionId() {
        return tillSessionId;
    }

    public void setTillSessionId(String tillSessionId) {
        this.tillSessionId = tillSessionId;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getCashierId() {
        return cashierId;
    }

    public void setCashierId(String cashierId) {
        this.cashierId = cashierId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public PosTransactionType getType() {
        return type;
    }

    public void setType(PosTransactionType type) {
        this.type = type;
    }

    public PosTransactionStatus getStatus() {
        return status;
    }

    public void setStatus(PosTransactionStatus status) {
        this.status = status;
    }

    public String getRefundOfId() {
        return refundOfId;
    }

    public void setRefundOfId(String refundOfId) {
        this.refundOfId = refundOfId;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public long getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(long subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }

    public long getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(long taxAmount) {
        this.taxAmount = taxAmount;
    }

    public long getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(long discountAmount) {
        this.discountAmount = discountAmount;
    }

    public long getManualDiscountAmount() {
        return manualDiscountAmount;
    }

    public void setManualDiscountAmount(long manualDiscountAmount) {
        this.manualDiscountAmount = manualDiscountAmount;
    }

    public String getManualDiscountReason() {
        return manualDiscountReason;
    }

    public void setManualDiscountReason(String manualDiscountReason) {
        this.manualDiscountReason = manualDiscountReason;
    }

    public String getManagerApprovalBy() {
        return managerApprovalBy;
    }

    public void setManagerApprovalBy(String managerApprovalBy) {
        this.managerApprovalBy = managerApprovalBy;
    }

    public String getManagerApprovalName() {
        return managerApprovalName;
    }

    public void setManagerApprovalName(String managerApprovalName) {
        this.managerApprovalName = managerApprovalName;
    }

    public Instant getManagerApprovalAt() {
        return managerApprovalAt;
    }

    public void setManagerApprovalAt(Instant managerApprovalAt) {
        this.managerApprovalAt = managerApprovalAt;
    }

    public long getSurchargeAmount() {
        return surchargeAmount;
    }

    public void setSurchargeAmount(long surchargeAmount) {
        this.surchargeAmount = surchargeAmount;
    }

    public long getSurchargeTaxAmount() {
        return surchargeTaxAmount;
    }

    public void setSurchargeTaxAmount(long surchargeTaxAmount) {
        this.surchargeTaxAmount = surchargeTaxAmount;
    }

    public boolean isSurchargeTaxable() {
        return surchargeTaxable;
    }

    public void setSurchargeTaxable(boolean surchargeTaxable) {
        this.surchargeTaxable = surchargeTaxable;
    }

    public String getSurchargeMethodId() {
        return surchargeMethodId;
    }

    public void setSurchargeMethodId(String surchargeMethodId) {
        this.surchargeMethodId = surchargeMethodId;
    }

    public String getSurchargeLabel() {
        return surchargeLabel;
    }

    public void setSurchargeLabel(String surchargeLabel) {
        this.surchargeLabel = surchargeLabel;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public long getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(long paidAmount) {
        this.paidAmount = paidAmount;
    }

    public long getChangeAmount() {
        return changeAmount;
    }

    public void setChangeAmount(long changeAmount) {
        this.changeAmount = changeAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isAgeVerified() {
        return ageVerified;
    }

    public void setAgeVerified(boolean ageVerified) {
        this.ageVerified = ageVerified;
    }

    public Instant getAgeVerifiedAt() {
        return ageVerifiedAt;
    }

    public void setAgeVerifiedAt(Instant ageVerifiedAt) {
        this.ageVerifiedAt = ageVerifiedAt;
    }

    public String getAgeVerifiedBy() {
        return ageVerifiedBy;
    }

    public void setAgeVerifiedBy(String ageVerifiedBy) {
        this.ageVerifiedBy = ageVerifiedBy;
    }

    public String getAgeIdType() {
        return ageIdType;
    }

    public void setAgeIdType(String ageIdType) {
        this.ageIdType = ageIdType;
    }

    public int getReprintCount() {
        return reprintCount;
    }

    public void setReprintCount(int reprintCount) {
        this.reprintCount = reprintCount;
    }

    public boolean isOfflineOrigin() {
        return offlineOrigin;
    }

    public void setOfflineOrigin(boolean offlineOrigin) {
        this.offlineOrigin = offlineOrigin;
    }

    public String getClientTxnUuid() {
        return clientTxnUuid;
    }

    public void setClientTxnUuid(String clientTxnUuid) {
        this.clientTxnUuid = clientTxnUuid;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public List<PosTransactionLine> getLines() {
        return lines;
    }

    public List<PosTender> getTenders() {
        return tenders;
    }
}

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
 * One METHOD within a single {@link SupplierPayment} — the split-payment line. Paying one bill
 * with Cash + Card is ONE payment recorded once; each tender resolves its own GL account so the
 * methods credit different ledgers inside one balanced voucher. {@code payment} is a real
 * same-slice FK; {@code paymentMethodId} is a loose cross-slice ULID ref.
 */
@Entity
@Table(name = "supplier_payment_tenders")
public class SupplierPaymentTender extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "payment_id", nullable = false)
    private SupplierPayment payment;

    /** ULID public id of the PaymentMethod (cross-slice, resolved app-side). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "payment_method_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String paymentMethodId;

    @Column(name = "payment_method_name", nullable = false, length = 100)
    private String paymentMethodName;

    @Column(name = "amount_amount", nullable = false)
    private long amountAmount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "amount_currency", nullable = false, length = 3, columnDefinition = "char(3)")
    private String amountCurrency;

    @Column(name = "reference_no", length = 100)
    private String referenceNo;

    public SupplierPaymentTender() {
    }

    public SupplierPayment getPayment() {
        return payment;
    }

    public void setPayment(SupplierPayment payment) {
        this.payment = payment;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public String getPaymentMethodName() {
        return paymentMethodName;
    }

    public void setPaymentMethodName(String paymentMethodName) {
        this.paymentMethodName = paymentMethodName;
    }

    public long getAmountAmount() {
        return amountAmount;
    }

    public void setAmountAmount(long amountAmount) {
        this.amountAmount = amountAmount;
    }

    public String getAmountCurrency() {
        return amountCurrency;
    }

    public void setAmountCurrency(String amountCurrency) {
        this.amountCurrency = amountCurrency;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }
}

package com.guru.erp.modules.procurement.bills.service;

import com.guru.erp.modules.procurement.bills.domain.SupplierPayment;
import com.guru.erp.platform.outbox.OutboxPublisher;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cross-module posting seam for supplier payments. Confirming (APPROVED) a payment posts a V-003
 * Purchase Payment voucher (DR Accounts Payable · CR each tender's cash/bank account, plus a
 * settlement-discount leg and a realized-FX plug) owned by finance; voiding reverses it.
 *
 * <p>Emitted via the platform {@link OutboxPublisher} so the slice never calls finance directly.
 * The per-tender GL split, PURCHASE_DISCOUNT / EXCHANGE_GAIN_LOSS legs, and fiscal-period gating
 * are the finance consumer's job — deferred here.
 */
@Service
public class PaymentPostingService {

    static final String AGGREGATE = "supplier_payment";
    static final String EVENT_POSTED = "procurement.payment.posted";
    static final String EVENT_REVERSED = "procurement.payment.reversed";

    private final OutboxPublisher outbox;

    public PaymentPostingService(OutboxPublisher outbox) {
        this.outbox = outbox;
    }

    /** Emitted on Draft → Approved — finance posts the V-003 payment voucher. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void emitPosted(SupplierPayment pay) {
        outbox.publish(AGGREGATE, pay.getPublicId(), EVENT_POSTED, payload(pay));
    }

    private Map<String, Object> payload(SupplierPayment pay) {
        return Map.of(
            "paymentId", pay.getPublicId(),
            "number", pay.getNumber(),
            "supplierId", pay.getSupplierId(),
            "poId", pay.getPoId() == null ? "" : pay.getPoId(),
            "currency", pay.getAmountCurrency(),
            "exchangeRate", pay.getExchangeRate().toPlainString(),
            "amountAmount", pay.getAmountAmount(),
            "baseAmount", pay.getBaseAmount(),
            "discountType", pay.getDiscountType() == null ? "" : pay.getDiscountType(),
            "discountValue", pay.getDiscountValue(),
            "tenders", tenders(pay));
    }

    private List<Map<String, Object>> tenders(SupplierPayment pay) {
        return pay.getTenders().stream().map(t -> Map.<String, Object>of(
            "tenderId", t.getPublicId(),
            "paymentMethodId", t.getPaymentMethodId(),
            "paymentMethodName", t.getPaymentMethodName(),
            "amountAmount", t.getAmountAmount(),
            "amountCurrency", t.getAmountCurrency())).toList();
    }
}

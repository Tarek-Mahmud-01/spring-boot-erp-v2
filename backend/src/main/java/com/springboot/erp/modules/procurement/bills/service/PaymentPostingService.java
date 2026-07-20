package com.springboot.erp.modules.procurement.bills.service;

import com.springboot.erp.modules.procurement.bills.domain.SupplierPayment;
import com.springboot.erp.platform.outbox.OutboxPublisher;
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
        Map<String, Object> p = new java.util.LinkedHashMap<>();
        p.put("paymentId", pay.getPublicId());
        p.put("number", pay.getNumber());
        p.put("supplierId", pay.getSupplierId());
        p.put("poId", pay.getPoId() == null ? "" : pay.getPoId());
        p.put("currency", pay.getAmountCurrency());
        p.put("exchangeRate", pay.getExchangeRate().toPlainString());
        p.put("amountAmount", pay.getAmountAmount());
        p.put("baseAmount", pay.getBaseAmount());
        p.put("discountType", pay.getDiscountType() == null ? "" : pay.getDiscountType());
        p.put("discountValue", pay.getDiscountValue());
        p.put("tenders", tenders(pay));
        return p;
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

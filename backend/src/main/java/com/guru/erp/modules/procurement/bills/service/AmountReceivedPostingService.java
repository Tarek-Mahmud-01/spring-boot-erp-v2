package com.guru.erp.modules.procurement.bills.service;

import com.guru.erp.modules.procurement.bills.domain.AmountReceived;
import com.guru.erp.platform.outbox.OutboxPublisher;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cross-module posting seam for refund receipts. Confirming an {@link AmountReceived} posts a
 * V-004 Receipt voucher (DR Bank/Cash · CR Accounts Payable, with a settlement-discount leg) owned
 * by finance; editing a confirmed receipt or voiding it reverses and re-posts.
 *
 * <p>Emitted via the platform {@link OutboxPublisher} so the slice never calls finance directly.
 * The discount leg, FX to base currency, idempotency and fiscal-period gating are the finance
 * consumer's job — deferred here.
 */
@Service
public class AmountReceivedPostingService {

    static final String AGGREGATE = "amount_received";
    static final String EVENT_POSTED = "procurement.amount_received.posted";
    static final String EVENT_REVERSED = "procurement.amount_received.reversed";

    private final OutboxPublisher outbox;

    public AmountReceivedPostingService(OutboxPublisher outbox) {
        this.outbox = outbox;
    }

    /** Emitted on Draft/Approved → Confirmed — finance posts the V-004 receipt voucher. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void emitPosted(AmountReceived rec) {
        outbox.publish(AGGREGATE, rec.getPublicId(), EVENT_POSTED, payload(rec));
    }

    /** Emitted when a confirmed receipt is edited or voided — finance reverses the live voucher. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void emitReversed(AmountReceived rec) {
        outbox.publish(AGGREGATE, rec.getPublicId(), EVENT_REVERSED, payload(rec));
    }

    private Map<String, Object> payload(AmountReceived rec) {
        return Map.of(
            "receiptId", rec.getPublicId(),
            "number", rec.getNumber(),
            "supplierId", rec.getSupplierId(),
            "poId", rec.getPoId() == null ? "" : rec.getPoId(),
            "currency", rec.getAmountCurrency(),
            "exchangeRate", rec.getExchangeRate().toPlainString(),
            "amountAmount", rec.getAmountAmount(),
            "baseAmount", rec.getBaseAmount(),
            "discountType", rec.getDiscountType() == null ? "" : rec.getDiscountType(),
            "discountValue", rec.getDiscountValue());
    }
}

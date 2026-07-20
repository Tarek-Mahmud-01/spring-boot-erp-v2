package com.springboot.erp.modules.pos.transactions.service;

import com.springboot.erp.modules.pos.transactions.domain.PosTender;
import com.springboot.erp.modules.pos.transactions.domain.PosTransaction;
import com.springboot.erp.modules.pos.transactions.domain.PosTransactionLine;
import com.springboot.erp.platform.outbox.OutboxPublisher;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cross-slice effects for POS sales. Instead of hard-calling the inventory slice (to decrement
 * stock) or the finance/GL module (to post the sale + COGS + surcharge legs) directly — which
 * would couple the slices and break the build — this service emits domain events to the platform
 * {@link OutboxPublisher}. Everything is referenced by {@code publicId}; downstream consumers
 * (deferred) perform the actual writes. Mirrors the reference {@code EVENT_POS_SALE_COMPLETED} /
 * {@code EVENT_POS_SALE_VOIDED} payloads: full line detail (qty + free_qty + weighed_qty_kg for
 * the stock decrement, cost/discount/promotion detail for GL + reporting) and the active tender
 * split (so the GL poster can debit cash vs card-clearing per method).
 */
@Service
public class TransactionPostingService {

    static final String AGGREGATE = "pos_transaction";
    static final String EVENT_SALE_COMPLETED = "pos.sale.completed";
    static final String EVENT_SALE_VOIDED = "pos.sale.voided";

    private final OutboxPublisher outbox;

    public TransactionPostingService(OutboxPublisher outbox) {
        this.outbox = outbox;
    }

    /** Emitted on COMPLETE — inventory decrements stock; finance posts the sale + COGS + surcharge. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void emitCompleted(PosTransaction txn) {
        outbox.publish(AGGREGATE, txn.getPublicId(), EVENT_SALE_COMPLETED, payload(txn, false));
    }

    /** Emitted on VOID of a previously-COMPLETED sale — downstream unwinds inventory + GL + loyalty. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void emitVoided(PosTransaction txn, boolean wasCompleted) {
        outbox.publish(AGGREGATE, txn.getPublicId(), EVENT_SALE_VOIDED, payload(txn, wasCompleted));
    }

    private Map<String, Object> payload(PosTransaction txn, boolean wasCompleted) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("transactionId", txn.getPublicId());
        m.put("registerId", txn.getRegisterId());
        m.put("locationId", txn.getLocationId());
        m.put("cashierId", txn.getCashierId() == null ? "" : txn.getCashierId());
        m.put("customerId", txn.getCustomerId() == null ? "" : txn.getCustomerId());
        m.put("currency", txn.getCurrency());
        m.put("status", txn.getStatus().name());
        m.put("wasCompleted", wasCompleted);
        m.put("subtotalAmount", txn.getSubtotalAmount());
        m.put("taxAmount", txn.getTaxAmount());
        m.put("discountAmount", txn.getDiscountAmount());
        m.put("surchargeAmount", txn.getSurchargeAmount());
        m.put("surchargeTaxAmount", txn.getSurchargeTaxAmount());
        m.put("totalAmount", txn.getTotalAmount());
        m.put("entryDate", (txn.getCompletedAt() == null ? java.time.Instant.now() : txn.getCompletedAt())
            .toString());
        m.put("tenders", tenders(txn));
        m.put("lines", lines(txn));
        return m;
    }

    private List<Map<String, Object>> tenders(PosTransaction txn) {
        return txn.getTenders().stream()
            .filter(t -> !t.isReversed())
            .map(TransactionPostingService::tender)
            .toList();
    }

    private static Map<String, Object> tender(PosTender t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("paymentMethodId", t.getPaymentMethodId());
        m.put("methodType", t.getMethodType());
        m.put("amountAmount", t.getAmountAmount());
        m.put("maskedPan", t.getMaskedPan() == null ? "" : t.getMaskedPan());
        return m;
    }

    private List<Map<String, Object>> lines(PosTransaction txn) {
        return txn.getLines().stream().map(TransactionPostingService::line).toList();
    }

    private static Map<String, Object> line(PosTransactionLine l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("lineId", l.getPublicId());
        m.put("productId", l.getProductId());
        m.put("variantId", l.getVariantId() == null ? "" : l.getVariantId());
        m.put("sku", l.getSku());
        m.put("name", l.getName());
        m.put("qty", l.getQty().toPlainString());
        m.put("freeQty", l.getFreeQty().toPlainString());
        m.put("weighedQtyKg", l.getWeighedQtyKg() == null ? null : l.getWeighedQtyKg().toPlainString());
        m.put("unitPriceAmount", l.getUnitPriceAmount());
        m.put("basePriceAmount", l.getBasePriceAmount());
        m.put("discountAmount", l.getDiscountAmount());
        m.put("promotionLabel", l.getPromotionLabel() == null ? "" : l.getPromotionLabel());
        m.put("lineNetAmount", l.getLineNetAmount());
        m.put("taxAmount", l.getTaxAmount());
        m.put("lineTotalAmount", l.getLineTotalAmount());
        return m;
    }
}

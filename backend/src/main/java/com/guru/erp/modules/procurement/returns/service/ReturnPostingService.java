package com.guru.erp.modules.procurement.returns.service;

import com.guru.erp.modules.procurement.returns.domain.SupplierReturn;
import com.guru.erp.modules.procurement.returns.domain.SupplierReturnLine;
import com.guru.erp.platform.outbox.OutboxPublisher;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cross-slice posting for supplier returns. When a return is CONFIRMED the reference
 * ({@code supplier_returns.py}) does two things beyond this aggregate:
 *
 * <ol>
 *   <li>relieves on-hand stock — one negative {@code RETURN} {@code StockLedger} row per line (plus a
 *       value-only {@code REVALUATION} row for any capitalised landed cost), owned by the inventory
 *       stock sub-slice;</li>
 *   <li>posts the V-007 Purchase Return (Debit Note) journal — DR AP / DR Freight / CR Inventory /
 *       CR GST — owned by the finance module.</li>
 * </ol>
 *
 * <p>To avoid hard cross-slice compile dependencies (which would break the build), this service
 * emits domain events to the platform {@link OutboxPublisher} instead of calling the ledger writer
 * or the GL poster directly. The inventory and finance modules consume the events and perform the
 * actual writes. Everything is referenced by {@code publicId}. This is the documented posting seam.
 *
 * <p>TODO(inventory + finance): implement the outbox consumers that (a) write the negative RETURN
 * StockLedger rows and (b) post/reverse the V-007 debit-note journal. Until then the ledger + GL
 * side-effects are deferred.
 */
@Service
public class ReturnPostingService {

    static final String AGGREGATE = "supplier_return";
    static final String EVENT_CONFIRMED = "procurement.return.confirmed";
    static final String EVENT_REVERSED = "procurement.return.reversed";

    private final OutboxPublisher outbox;

    public ReturnPostingService(OutboxPublisher outbox) {
        this.outbox = outbox;
    }

    /**
     * Emitted when a return is confirmed (created-as-confirmed or DRAFT → CONFIRMED). The inventory
     * slice relieves stock; finance posts the V-007 debit note.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void emitConfirmed(SupplierReturn r) {
        outbox.publish(AGGREGATE, r.getPublicId(), EVENT_CONFIRMED, payload(r, "RETURN"));
    }

    /**
     * Emitted when a confirmed return is reversed (deleted, or edited before re-posting). The
     * inventory slice re-credits on-hand; finance reverses the V-007 debit note.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void emitReversed(SupplierReturn r) {
        outbox.publish(AGGREGATE, r.getPublicId(), EVENT_REVERSED, payload(r, "RETURN_REVERSAL"));
    }

    private Map<String, Object> payload(SupplierReturn r, String movementType) {
        return Map.of(
            "returnId", r.getPublicId(),
            "number", r.getNumber(),
            "supplierId", r.getSupplierId(),
            "grnId", r.getGrnId(),
            "movementType", movementType,
            "debitNoteAmount", r.getDebitNote().amountMinor(),
            "debitNoteCurrency", r.getDebitNote().currency(),
            "baseDebitNoteAmount", r.getBaseDebitNote().amountMinor(),
            "exchangeRate", r.getExchangeRate().toPlainString(),
            "lines", lines(r));
    }

    private List<Map<String, Object>> lines(SupplierReturn r) {
        return r.getLines().stream().map(ReturnPostingService::line).toList();
    }

    private static Map<String, Object> line(SupplierReturnLine l) {
        return Map.of(
            "lineId", l.getPublicId(),
            "grnLineId", l.getGrnLineId() == null ? "" : l.getGrnLineId(),
            "variantId", l.getVariantId() == null ? "" : l.getVariantId(),
            "qty", l.getQty().toPlainString());
    }
}

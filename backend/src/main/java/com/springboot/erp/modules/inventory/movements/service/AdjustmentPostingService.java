package com.springboot.erp.modules.inventory.movements.service;

import com.springboot.erp.modules.inventory.movements.domain.StockAdjustment;
import com.springboot.erp.modules.inventory.movements.domain.StockAdjustmentLine;
import com.springboot.erp.platform.outbox.OutboxPublisher;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cross-slice posting for stock adjustments. When an adjustment is POSTED the reference writes an
 * ADJUSTMENT/WRITE_OFF {@code StockLedger} row per line (owned by the stock sub-slice) and a GL
 * journal entry (owned by the finance module) that debits/credits the variance account.
 *
 * <p>To avoid hard cross-slice compile dependencies, this service emits a domain event to the
 * platform {@link OutboxPublisher} instead of calling the ledger writer or the GL poster directly.
 * The stock slice and finance module consume the event (future outbox consumers) and perform the
 * actual writes. Everything is referenced by {@code publicId}. This is the documented posting seam.
 *
 * <p>TODO(stock-slice + finance): implement the outbox consumers that (a) write the ADJUSTMENT
 * StockLedger rows and (b) create the variance journal entry, then set {@code journalEntryId} back
 * on the adjustment. Until then the ledger + GL side-effects are deferred.
 */
@Service
public class AdjustmentPostingService {

    static final String AGGREGATE = "stock_adjustment";
    static final String EVENT_POSTED = "inventory.adjustment.posted";
    static final String EVENT_REVERSED = "inventory.adjustment.reversed";

    private final OutboxPublisher outbox;

    public AdjustmentPostingService(OutboxPublisher outbox) {
        this.outbox = outbox;
    }

    /** Emitted on POST — the stock slice writes ADJUSTMENT ledger rows; finance posts the journal. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void emitPosted(StockAdjustment a) {
        outbox.publish(AGGREGATE, a.getPublicId(), EVENT_POSTED, payload(a, "ADJUSTMENT"));
    }

    /** Emitted on reversal (delete of a posted adjustment) — the stock slice unwinds the movements. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void emitReversed(StockAdjustment a) {
        outbox.publish(AGGREGATE, a.getPublicId(), EVENT_REVERSED, payload(a, "ADJUSTMENT_REVERSAL"));
    }

    private Map<String, Object> payload(StockAdjustment a, String movementType) {
        return Map.of(
            "adjustmentId", a.getPublicId(),
            "number", a.getNumber(),
            "locationId", a.getLocationId(),
            "reason", a.getReason(),
            "varianceAccountId", a.getVarianceAccountId() == null ? "" : a.getVarianceAccountId(),
            "movementType", movementType,
            "lines", lines(a));
    }

    private List<Map<String, Object>> lines(StockAdjustment a) {
        return a.getLines().stream().map(AdjustmentPostingService::line).toList();
    }

    private static Map<String, Object> line(StockAdjustmentLine l) {
        return Map.of(
            "lineId", l.getPublicId(),
            "productId", l.getProductId(),
            "variantId", l.getVariantId() == null ? "" : l.getVariantId(),
            "qtyVariance", l.getQtyVariance().toPlainString(),
            "unitCostAmount", l.getUnitCost().amountMinor(),
            "unitCostCurrency", l.getUnitCost().currency());
    }
}

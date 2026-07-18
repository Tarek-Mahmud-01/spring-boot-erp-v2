package com.guru.erp.modules.procurement.landed.service;

import com.guru.erp.modules.procurement.landed.domain.LandedCost;
import com.guru.erp.modules.procurement.landed.domain.LandedCostAllocation;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cross-slice posting for landed costs. Applying a landed cost capitalises freight/duty into
 * inventory: the reference debits INVENTORY_ASSET / credits AP (a V-011 GL voucher) and raises the
 * moving-average of the received units via value-only REVALUATION stock-ledger rows, so COGS picks
 * up the landed cost when the goods sell.
 *
 * <p>To keep the vertical slice free of hard cross-module dependencies, this service emits a domain
 * event to the platform {@link com.guru.erp.platform.outbox.OutboxPublisher} instead of calling the
 * stock revaluation writer or GL poster directly. The inventory + finance modules consume the event
 * (future outbox consumers) and perform the actual revaluation / journal. Everything is referenced
 * by {@code publicId}.
 *
 * <p>TODO(inventory + finance): implement the outbox consumers that (a) post the value-only
 * REVALUATION stock-ledger rows per allocation and (b) create the V-011 landed-cost journal. Until
 * then the revaluation + GL side-effects are deferred.
 */
@Service
public class LandedCostPostingService {

    static final String AGGREGATE = "landed_cost";
    static final String EVENT_APPLIED = "procurement.landed_cost.applied";
    static final String EVENT_REVERSED = "procurement.landed_cost.reversed";

    private final com.guru.erp.platform.outbox.OutboxPublisher outbox;

    public LandedCostPostingService(com.guru.erp.platform.outbox.OutboxPublisher outbox) {
        this.outbox = outbox;
    }

    /** Emitted on APPLY — inventory revalues stock; finance posts the V-011 capitalisation journal. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void emitApplied(LandedCost lc) {
        outbox.publish(AGGREGATE, lc.getPublicId(), EVENT_APPLIED, payload(lc, "LANDED_COST"));
    }

    /** Emitted on reversal (delete of an applied cost) — inventory unwinds the revaluation. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void emitReversed(LandedCost lc) {
        outbox.publish(AGGREGATE, lc.getPublicId(), EVENT_REVERSED,
            payload(lc, "LANDED_COST_REVERSAL"));
    }

    private Map<String, Object> payload(LandedCost lc, String movementType) {
        return Map.of(
            "landedCostId", lc.getPublicId(),
            "invoiceNumber", lc.getInvoiceNumber() == null ? "" : lc.getInvoiceNumber(),
            "chargeType", lc.getChargeType().name(),
            "grnId", lc.getGrnId() == null ? "" : lc.getGrnId(),
            "poId", lc.getPoId() == null ? "" : lc.getPoId(),
            "supplierId", lc.getSupplierId() == null ? "" : lc.getSupplierId(),
            "baseAmount", lc.getBaseAmount().amountMinor(),
            "baseCurrency", lc.getBaseAmount().currency(),
            "movementType", movementType,
            "allocations", allocations(lc));
    }

    private List<Map<String, Object>> allocations(LandedCost lc) {
        return lc.getAllocations().stream().map(LandedCostPostingService::allocation).toList();
    }

    private static Map<String, Object> allocation(LandedCostAllocation a) {
        return Map.of(
            "allocationId", a.getPublicId(),
            "grnLineId", a.getGrnLineId() == null ? "" : a.getGrnLineId(),
            "poLineId", a.getPoLineId() == null ? "" : a.getPoLineId(),
            "allocatedAmount", a.getAllocatedAmount().amountMinor(),
            "allocatedCurrency", a.getAllocatedAmount().currency(),
            "allocQty", a.getAllocQty() == null ? "" : a.getAllocQty().toPlainString());
    }
}

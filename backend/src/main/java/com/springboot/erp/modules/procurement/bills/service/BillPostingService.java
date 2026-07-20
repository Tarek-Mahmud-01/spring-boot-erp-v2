package com.springboot.erp.modules.procurement.bills.service;

import com.springboot.erp.modules.procurement.bills.domain.SupplierBill;
import com.springboot.erp.platform.outbox.OutboxPublisher;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cross-module posting seam for supplier bills. When a bill is approved the reference posts a
 * V-002 purchase-payable journal (DR Inventory/GST · CR Accounts Payable) — GRN-first — or a
 * bill-first Invoice-Pending-Receipt voucher, and revalues on-hand inventory. Both are owned by
 * the finance / stock modules.
 *
 * <p>To keep the vertical slice free of hard cross-module dependencies, this service emits a
 * domain event to the platform {@link OutboxPublisher} instead of calling the GL poster or the
 * revaluation writer directly. Finance + stock consume the event and perform the actual GL /
 * ledger writes, keyed entirely by {@code publicId}. This is the documented posting seam.
 *
 * <p>Deferred to the outbox consumers (finance/stock): the V-002 vs bill-first branch, PPV /
 * GRN-clearing legs, FX to base currency, fiscal-period gating, and the V-015 revaluation. This
 * slice decides only the bill's own status; it never books the journal itself.
 */
@Service
public class BillPostingService {

    static final String AGGREGATE = "supplier_bill";
    static final String EVENT_POSTED = "procurement.bill.posted";
    static final String EVENT_REVERSED = "procurement.bill.reversed";

    private final OutboxPublisher outbox;

    public BillPostingService(OutboxPublisher outbox) {
        this.outbox = outbox;
    }

    /**
     * Emitted when a bill is approved (auto after a clean 3-way match, or manual). Finance posts
     * the payable voucher; stock revalues on-hand to the bill cost. {@code billFirst} tells the
     * consumer to book Invoice-Pending-Receipt instead of the standard payable.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void emitPosted(SupplierBill bill, boolean billFirst) {
        outbox.publish(AGGREGATE, bill.getPublicId(), EVENT_POSTED, payload(bill, billFirst));
    }

    /** Emitted on delete of a posted bill — finance reverses the payable, stock re-values back. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void emitReversed(SupplierBill bill) {
        outbox.publish(AGGREGATE, bill.getPublicId(), EVENT_REVERSED, payload(bill, false));
    }

    private Map<String, Object> payload(SupplierBill bill, boolean billFirst) {
        return Map.of(
            "billId", bill.getPublicId(),
            "number", bill.getNumber(),
            "supplierId", bill.getSupplierId(),
            "poId", bill.getPoId() == null ? "" : bill.getPoId(),
            "currency", bill.getCurrency(),
            "exchangeRate", bill.getExchangeRate().toPlainString(),
            "subtotalAmount", bill.getSubtotalAmount(),
            "taxAmount", bill.getTaxAmount(),
            "totalAmount", bill.getTotalAmount(),
            "billFirst", billFirst);
    }
}

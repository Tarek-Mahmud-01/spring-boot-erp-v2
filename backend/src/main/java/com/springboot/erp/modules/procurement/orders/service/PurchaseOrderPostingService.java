package com.springboot.erp.modules.procurement.orders.service;

import com.springboot.erp.modules.procurement.orders.domain.PoStatus;
import com.springboot.erp.modules.procurement.orders.domain.PurchaseOrder;
import com.springboot.erp.modules.procurement.orders.domain.PurchaseOrderLine;
import com.springboot.erp.platform.outbox.OutboxPublisher;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cross-slice effects for purchase orders. Instead of hard-calling the GRN / goods-receipt slice,
 * the AP / finance module, or an email/PDF renderer directly (which would couple the slices and
 * break the build), this service emits domain events to the platform {@link OutboxPublisher}.
 * Everything is referenced by {@code publicId}; downstream consumers perform the actual writes.
 *
 * <p>Deferred consumers (documented seam): (a) the Direct-PO chain — when {@code isDirect} is set,
 * auto-create the GoodsReceipt + SupplierBill + payments; (b) a "received" consumer that guards
 * cancellation and rolls up receipt quantities; (c) email/PDF rendering of the PO document.
 */
@Service
public class PurchaseOrderPostingService {

    static final String AGGREGATE = "purchase_order";
    static final String EVENT_CREATED = "procurement.purchase_order.created";
    static final String EVENT_STATUS_CHANGED = "procurement.purchase_order.status_changed";

    private final OutboxPublisher outbox;

    public PurchaseOrderPostingService(OutboxPublisher outbox) {
        this.outbox = outbox;
    }

    /**
     * Emitted on create. Carries {@code isDirect} so the Direct-PO consumer can complete the
     * GRN + bill + payment chain (deferred). Also lets email/PDF rendering hook in.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void emitCreated(PurchaseOrder po) {
        outbox.publish(AGGREGATE, po.getPublicId(), EVENT_CREATED, payload(po, null, null));
    }

    /**
     * Emitted on each workflow transition. The APPROVED/SENT/RECEIVED/CANCELLED consumers own the
     * receipt-quantity rollups and the received-goods cancellation guard (deferred).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void emitStatusChanged(PurchaseOrder po, PoStatus to, String reason) {
        outbox.publish(AGGREGATE, po.getPublicId(), EVENT_STATUS_CHANGED, payload(po, to, reason));
    }

    private Map<String, Object> payload(PurchaseOrder po, PoStatus to, String reason) {
        return Map.of(
            "purchaseOrderId", po.getPublicId(),
            "number", po.getNumber(),
            "supplierId", po.getSupplierId(),
            "locationId", po.getLocationId() == null ? "" : po.getLocationId(),
            "currency", po.getCurrency(),
            "status", po.getStatus().wire(),
            "toStatus", to == null ? "" : to.wire(),
            "reason", reason == null ? "" : reason,
            "isDirect", po.isDirect(),
            "lines", lines(po));
    }

    private List<Map<String, Object>> lines(PurchaseOrder po) {
        return po.getLines().stream().map(PurchaseOrderPostingService::line).toList();
    }

    private static Map<String, Object> line(PurchaseOrderLine l) {
        return Map.of(
            "lineId", l.getPublicId(),
            "lineNo", l.getLineNo(),
            "productId", l.getProductId() == null ? "" : l.getProductId(),
            "variantId", l.getVariantId() == null ? "" : l.getVariantId(),
            "qtyOrdered", l.getQtyOrdered().toPlainString(),
            "unitPriceAmount", l.getUnitPrice().amountMinor(),
            "unitPriceCurrency", l.getUnitPrice().currency(),
            "lineTotalAmount", l.getLineTotal().amountMinor());
    }
}

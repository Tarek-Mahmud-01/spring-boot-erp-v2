package com.springboot.erp.modules.inventory.movements.service;

import com.springboot.erp.modules.inventory.movements.domain.StockTransfer;
import com.springboot.erp.modules.inventory.movements.domain.StockTransferLine;
import com.springboot.erp.platform.outbox.OutboxPublisher;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cross-slice posting for stock transfers. When a transfer is confirmed (stock leaves the source)
 * or received (stock lands at the destination), the reference writes {@code StockLedger} rows
 * (TRANSFER_OUT / TRANSFER_IN). Those rows are owned by the <b>stock</b> sub-slice, not this one.
 *
 * <p>To avoid a hard cross-slice compile dependency (per the vertical-slice rule and the task
 * brief), this service does NOT call a ledger writer directly. Instead it emits a domain event to
 * the platform {@link OutboxPublisher}; the stock slice's ledger writer (a future outbox consumer)
 * translates the event into the actual TRANSFER_OUT / TRANSFER_IN ledger movements. Everything is
 * referenced by {@code publicId}. This is the documented cross-slice posting seam.
 *
 * <p>TODO(stock-slice): implement the outbox consumer that writes StockLedger rows from these
 * events. Until then the events accumulate in the outbox and the ledger side-effect is deferred.
 */
@Service
public class TransferPostingService {

    static final String AGGREGATE = "stock_transfer";
    static final String EVENT_CONFIRMED = "inventory.transfer.confirmed";
    static final String EVENT_RECEIVED = "inventory.transfer.received";

    private final OutboxPublisher outbox;

    public TransferPostingService(OutboxPublisher outbox) {
        this.outbox = outbox;
    }

    /** Emitted on confirm — the stock slice should write TRANSFER_OUT rows out of the source. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void emitConfirmed(StockTransfer t) {
        outbox.publish(AGGREGATE, t.getPublicId(), EVENT_CONFIRMED, Map.of(
            "transferId", t.getPublicId(),
            "number", t.getNumber(),
            "sourceLocationId", t.getSourceLocationId(),
            "destinationLocationId", t.getDestinationLocationId(),
            "movementType", "TRANSFER_OUT",
            "lines", lines(t)));
    }

    /** Emitted on receive — the stock slice should write TRANSFER_IN rows into the destination. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void emitReceived(StockTransfer t) {
        outbox.publish(AGGREGATE, t.getPublicId(), EVENT_RECEIVED, Map.of(
            "transferId", t.getPublicId(),
            "number", t.getNumber(),
            "sourceLocationId", t.getSourceLocationId(),
            "destinationLocationId", t.getDestinationLocationId(),
            "movementType", "TRANSFER_IN",
            "lines", lines(t)));
    }

    private List<Map<String, Object>> lines(StockTransfer t) {
        return t.getLines().stream().map(TransferPostingService::line).toList();
    }

    private static Map<String, Object> line(StockTransferLine l) {
        return Map.of(
            "lineId", l.getPublicId(),
            "productId", l.getProductId(),
            "variantId", l.getVariantId() == null ? "" : l.getVariantId(),
            "qtySent", l.getQtySent().toPlainString(),
            "qtyReceived", l.getQtyReceived().toPlainString(),
            "qtyShort", l.getQtyShort().toPlainString(),
            "qtyDamaged", l.getQtyDamaged().toPlainString());
    }
}

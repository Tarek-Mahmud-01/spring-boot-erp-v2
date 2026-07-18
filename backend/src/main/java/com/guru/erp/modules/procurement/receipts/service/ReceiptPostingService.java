package com.guru.erp.modules.procurement.receipts.service;

import com.guru.erp.modules.procurement.receipts.domain.GoodsReceipt;
import com.guru.erp.modules.procurement.receipts.domain.GoodsReceiptLine;
import com.guru.erp.platform.outbox.OutboxPublisher;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cross-slice posting for goods receipts. When a receipt is confirmed ("received") the reference
 * writes one RECEIPT {@code StockLedger} row per usable line (owned by the inventory stock
 * sub-slice), a {@code ProductBatch} row, and — in some flows — a GL journal (owned by finance).
 *
 * <p>To avoid hard cross-slice compile dependencies, this service emits a domain event to the
 * platform {@link OutboxPublisher} instead of calling the ledger writer or GL poster directly. The
 * stock slice (and finance module) consume the event and perform the actual writes, referencing
 * everything by {@code publicId}. This is the documented posting seam — the receipts slice never
 * hard-calls the inventory slice.
 *
 * <p>TODO(stock-slice + finance): implement the outbox consumers that (a) write the RECEIPT
 * StockLedger + ProductBatch rows and (b) recompute PO received totals / status. Until then the
 * ledger, batch, and PO-rollup side-effects are deferred.
 */
@Service
public class ReceiptPostingService {

    static final String AGGREGATE = "goods_receipt";
    static final String EVENT_RECEIVED = "procurement.goods_receipt.received";
    static final String EVENT_REVERSED = "procurement.goods_receipt.reversed";

    private final OutboxPublisher outbox;

    public ReceiptPostingService(OutboxPublisher outbox) {
        this.outbox = outbox;
    }

    /**
     * Emitted on confirm (→ Received) — the stock slice writes RECEIPT ledger + batch rows and
     * rolls received quantities up to the PO, recomputing its status.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void emitReceived(GoodsReceipt grn) {
        outbox.publish(AGGREGATE, grn.getPublicId(), EVENT_RECEIVED, payload(grn, "RECEIPT"));
    }

    /**
     * Emitted on delete of a confirmed receipt — the stock slice unwinds the RECEIPT movements and
     * rolls back the PO received totals / status.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void emitReversed(GoodsReceipt grn) {
        outbox.publish(AGGREGATE, grn.getPublicId(), EVENT_REVERSED, payload(grn, "RECEIPT_REVERSAL"));
    }

    private Map<String, Object> payload(GoodsReceipt grn, String movementType) {
        return Map.of(
            "grnId", grn.getPublicId(),
            "number", grn.getNumber(),
            "poId", grn.getPoId(),
            "locationId", grn.getLocationId(),
            "receivedAt", grn.getReceivedAt().toString(),
            "movementType", movementType,
            "lines", lines(grn));
    }

    private List<Map<String, Object>> lines(GoodsReceipt grn) {
        return grn.getLines().stream()
            .filter(l -> l.usableQty().signum() > 0)
            .map(ReceiptPostingService::line)
            .toList();
    }

    private static Map<String, Object> line(GoodsReceiptLine l) {
        BigDecimal usable = l.usableQty();
        return Map.of(
            "lineId", l.getPublicId(),
            "poLineId", l.getPoLineId() == null ? "" : l.getPoLineId(),
            "variantId", l.getVariantId() == null ? "" : l.getVariantId(),
            "qtyUsable", usable.toPlainString(),
            "qtyReceived", (l.getQtyReceived() == null ? BigDecimal.ZERO : l.getQtyReceived()).toPlainString(),
            "batchNo", l.getBatchNo() == null ? "" : l.getBatchNo(),
            "supplierBarcode", l.getSupplierBarcode() == null ? "" : l.getSupplierBarcode());
    }
}

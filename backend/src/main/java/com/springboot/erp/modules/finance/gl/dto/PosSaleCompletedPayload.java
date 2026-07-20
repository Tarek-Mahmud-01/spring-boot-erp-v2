package com.springboot.erp.modules.finance.gl.dto;

import java.time.Instant;
import java.util.List;

/**
 * Already-parsed shape of the {@code pos.sale.completed} / {@code pos.sale.voided} outbox event
 * payload (reference {@code app.finance.integrations.pos_gl_poster}, mirrors
 * {@code TransactionPostingService.payload(...)} on the POS producer side). This slice does NOT
 * poll the outbox or deserialize raw JSON itself — a future integration/outbox-relay layer reads the
 * stored event, maps it to this record, and calls {@code GlPostingConsumerService.postPosSale(...)}.
 *
 * <p>Deliberately minimal compared to the reference's full receipt-level poster (tenders, per-line
 * discount/promotion detail, COGS at moving-average cost, surcharge...) — that requires resolving
 * account mappings, per-product cost, and payment-method GL accounts, all of which belong to slices
 * not yet ported. This DTO carries exactly what the GL-core consumer needs to post a single
 * revenue-vs-cash line pair idempotently: the source aggregate id (for {@code GlPostingLog}), the
 * company/location, and pre-aggregated net/tax/total amounts in minor units (never re-derived here —
 * CLAUDE.md §5 "never re-round a stored amount").
 */
public record PosSaleCompletedPayload(
    /** {@code pos_transaction.publicId} — the GlPostingLog {@code sourceRef} / idempotency key. */
    String transactionId,
    String companyId,
    String locationId,
    String cashierId,
    String currency,
    /** Pre-tax, pre-surcharge net sales amount (minor units) — summed verbatim from the receipt lines. */
    long netAmount,
    /** GST/VAT collected on the sale (minor units). */
    long taxAmount,
    /** Cash/card tender total actually collected (minor units); drives the Dr cash-or-clearing leg. */
    long tenderAmount,
    Instant entryDate,
    String requestId
) {
}

package com.guru.erp.modules.crm.loyalty.dto;

import com.guru.erp.modules.crm.loyalty.domain.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** DTOs for the purchase-history projection (reference US-041 / FR-215-217). */
public final class CustomerTransactionDtos {

    private CustomerTransactionDtos() {
    }

    public record TransactionLine(
        String sku,
        String name,
        long qty,
        long unitPriceAmount,
        long lineAmount,
        String categoryId
    ) {
    }

    public record TransactionPayment(
        String method,
        String maskedPan,
        long amount
    ) {
    }

    /**
     * Stand-in ingestion request for the POS / Sales outbox feed, mirroring the
     * reference {@code CustomerTransactionIngestRequest}. Payment data must
     * arrive already masked (FR-217) — the service re-masks defensively so a raw
     * PAN never persists.
     */
    public record CustomerTransactionIngestRequest(
        @NotBlank @Size(min = 1, max = 40) String receiptNumber,
        TransactionType type,
        Instant occurredAt,
        @PositiveOrZero long totalAmount,
        Long subtotalAmount,
        @NotBlank @Size(min = 3, max = 3) String totalCurrency,
        @Size(max = 26) String locationId,
        List<TransactionPayment> paymentSummary,
        List<TransactionLine> lines,
        @Size(max = 26) String refundOfId
    ) {
    }

    public record CustomerTransactionResponse(
        String id,
        String customerId,
        String receiptNumber,
        TransactionType type,
        Instant occurredAt,
        long totalAmount,
        Long subtotalAmount,
        String currency,
        String locationId,
        List<TransactionPayment> paymentSummary,
        List<TransactionLine> lines,
        String refundOfId
    ) {
    }
}

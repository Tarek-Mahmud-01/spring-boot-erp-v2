package com.guru.erp.modules.pos.auxiliary.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Wire DTOs for PosParkedSale (records — ARCHITECTURE.md §2). Cross-slice ids (transaction,
 * register, location, user) are ULID {@code char(26)} strings.
 */
public final class ParkedSaleDtos {

    private ParkedSaleDtos() {
    }

    /** POST /api/pos/transactions/{publicId}/park body — US-035 FR-182. */
    public record ParkSaleRequest(
        @Size(max = 255) String note
    ) {
    }

    /** POST /api/pos/parked-sales/{parkCode}/resume path — no body; the code carries the request. */
    public record ResumeParkedSaleRequest(
        @NotNull @Size(min = 1, max = 12) String parkCode
    ) {
    }

    /** Parked-sale read shape — a lightweight cart summary, not the full transaction. */
    public record ParkedSaleResponse(
        String id,
        String parkCode,
        String transactionId,
        String registerId,
        String locationId,
        String note,
        int lineCount,
        long totalAmount,
        String currency,
        Instant parkedAt,
        Instant expiresAt,
        boolean resumed
    ) {
    }
}

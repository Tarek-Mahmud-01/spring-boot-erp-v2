package com.guru.erp.modules.pos.auxiliary.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;

/**
 * Wire DTOs for PosEvent (records — ARCHITECTURE.md §2). Cross-slice ids (register, transaction,
 * reviewer) are ULID {@code char(26)} strings.
 */
public final class PosEventDtos {

    private PosEventDtos() {
    }

    /** Record an arbitrary POS-domain event (age refusal, peripheral failure, ...). */
    public record RecordEventRequest(
        @NotNull String type,
        @Size(min = 26, max = 26) String registerId,
        @Size(min = 26, max = 26) String transactionId,
        Map<String, Object> payload,
        boolean needsReview
    ) {
    }

    /** Mark an event reviewed by the current (or a nominated) reviewer. */
    public record ReviewEventRequest(
        @Size(min = 26, max = 26) String reviewedBy
    ) {
    }

    /** A POS operational event (FR-25.8 / FR-AU-013 timeline row). */
    public record PosEventResponse(
        String id,
        String type,
        String registerId,
        String transactionId,
        Map<String, Object> payload,
        boolean needsReview,
        String reviewedBy,
        Instant reviewedAt,
        Instant createdAt,
        String createdBy
    ) {
    }
}

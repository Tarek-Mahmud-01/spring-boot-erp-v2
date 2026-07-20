package com.springboot.erp.modules.finance.gl.dto;

/** Wire DTO for the {@code GlPostingLog} idempotency/reconciliation ledger. */
public final class GlPostingLogDtos {

    private GlPostingLogDtos() {
    }

    public record GlPostingLogResponse(
        String id,
        String companyId,
        String sourceKind,
        String sourceRef,
        String eventType,
        String journalEntryId,
        String status,
        int attempts,
        String lastError
    ) {
    }
}

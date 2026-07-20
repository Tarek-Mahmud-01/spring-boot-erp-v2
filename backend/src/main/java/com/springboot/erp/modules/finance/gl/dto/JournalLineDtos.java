package com.springboot.erp.modules.finance.gl.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Wire DTOs for a single {@code JournalLine} (reference {@code JournalLineInput} / {@code JournalLineResponse}). */
public final class JournalLineDtos {

    private JournalLineDtos() {
    }

    /**
     * One line of a create/replace-lines request. Exactly one of {@code debit}/{@code credit} must
     * be strictly positive — {@code BalancingService} enforces the full xor invariant (this
     * annotation only rules out negative amounts; zero/zero and both-positive slip through Bean
     * Validation and are caught by the service).
     */
    public record JournalLineRequest(
        @NotNull @Size(min = 26, max = 26) String accountId,
        String holderType,
        @Size(min = 26, max = 26) String holderId,
        @Size(max = 500) String narration,
        @Min(0) long debit,
        @Min(0) long credit,
        @NotNull @Size(min = 3, max = 3) String currency,
        /** Multiplier to the company base currency; defaults to 1 (server-side) when omitted. */
        BigDecimal exchangeRate,
        @Size(min = 26, max = 26) String locationId
    ) {
    }

    public record JournalLineResponse(
        String id,
        String accountId,
        String holderType,
        String holderId,
        String narration,
        long debit,
        long credit,
        String currency,
        BigDecimal exchangeRate,
        long baseDebit,
        long baseCredit,
        String locationId
    ) {
    }
}

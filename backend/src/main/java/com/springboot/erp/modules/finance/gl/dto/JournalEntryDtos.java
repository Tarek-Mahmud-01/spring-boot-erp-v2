package com.springboot.erp.modules.finance.gl.dto;

import com.springboot.erp.modules.finance.gl.dto.JournalLineDtos.JournalLineRequest;
import com.springboot.erp.modules.finance.gl.dto.JournalLineDtos.JournalLineResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Wire DTOs for {@code JournalEntry} (reference {@code JournalEntryCreateRequest} / {@code JournalEntryResponse}). */
public final class JournalEntryDtos {

    private JournalEntryDtos() {
    }

    /** POST body — create a DRAFT journal entry. Must carry &gt;= 2 lines that already balance. */
    public record JournalEntryCreateRequest(
        @NotNull @Size(min = 26, max = 26) String companyId,
        @Size(min = 26, max = 26) String locationId,
        @NotNull @Size(max = 10) String voucherType,
        @NotNull LocalDate entryDate,
        @Size(max = 200) String reference,
        @Size(max = 1000) String narration,
        @NotNull @Valid List<JournalLineRequest> lines
    ) {
    }

    /** PATCH body — a DRAFT-only edit. Optimistic-lock {@code version} is required. */
    public record JournalEntryUpdateRequest(
        @NotNull long version,
        LocalDate entryDate,
        String reference,
        String narration,
        String voucherType,
        String locationId,
        @Valid List<JournalLineRequest> lines
    ) {
    }

    /** POST .../reverse body. */
    public record JournalEntryReverseRequest(
        @NotNull LocalDate entryDate,
        String narration
    ) {
    }

    public record JournalEntryResponse(
        String id,
        String companyId,
        String locationId,
        String voucherType,
        String voucherNumber,
        LocalDate entryDate,
        String periodCode,
        String reference,
        String narration,
        String status,
        List<JournalLineResponse> lines,
        long totalDebit,
        long totalCredit,
        boolean balanced,
        String reversedById,
        Instant postedAt,
        String postedBy,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy,
        long version
    ) {
    }
}

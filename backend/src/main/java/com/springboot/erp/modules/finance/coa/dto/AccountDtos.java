package com.springboot.erp.modules.finance.coa.dto;

import com.springboot.erp.modules.finance.coa.domain.AccountPostingType;
import com.springboot.erp.modules.finance.coa.domain.AccountStatus;
import com.springboot.erp.modules.finance.coa.domain.AccountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * Wire DTOs for the chart-of-accounts nested-set tree (reference
 * {@code app.finance.schemas} AccountCreateRequest / AccountUpdateRequest /
 * AccountResponse / CoaImportRequest / CoaImportResponse). Records only, per
 * ARCHITECTURE.md §2. All ids are ULIDs; the {@code lft}/{@code rgt}/{@code depth}
 * nested-set index is echoed on every response so clients can run their own
 * ancestor/descendant containment check without a round-trip.
 */
public final class AccountDtos {

    private AccountDtos() {
    }

    /** Account codes are looked up by exact match against the (company_id, code)
     *  unique index — limited to common bookkeeping code chars: alnum + dash +
     *  underscore + dot (reference {@code AccountCreateRequest._code_clean}). */
    private static final String CODE_PATTERN = "^[A-Za-z0-9\\-_.]+$";

    /** ISO-4217 codes are 3 uppercase letters (reference {@code _currency_upper}). */
    private static final String CURRENCY_PATTERN = "^[A-Za-z]{3}$";

    /** POST /api/finance/accounts body. */
    public record AccountCreateRequest(
        @NotBlank @Size(min = 26, max = 26) String companyId,
        @NotBlank @Size(min = 1, max = 20) @Pattern(regexp = CODE_PATTERN,
            message = "Code may only contain letters, digits, '-', '_' or '.'.") String code,
        @NotBlank @Size(min = 1, max = 200) String name,
        @NotNull AccountType type,
        @Size(min = 26, max = 26) String parentId,
        AccountPostingType postingType,
        @Pattern(regexp = CURRENCY_PATTERN, message = "Currency must be a 3-letter ISO-4217 code.") String currency,
        @Min(0) long openingDebitAmount,
        @Min(0) long openingCreditAmount,
        Instant openingDate,
        @Size(min = 26, max = 26) String openingLocationId
    ) {
    }

    /**
     * PATCH /api/finance/accounts/{id} body — every field optional. Re-parenting
     * is NOT accepted here — it always goes through the dedicated
     * {@code POST /{accountId}/move} endpoint (see {@link MoveAccountRequest}),
     * exactly like the reference's split between "field update" and
     * "subtree move" inside {@code update_account}.
     */
    public record AccountUpdateRequest(
        @Size(min = 1, max = 200) String name,
        AccountPostingType postingType,
        @Pattern(regexp = CURRENCY_PATTERN, message = "Currency must be a 3-letter ISO-4217 code.") String currency,
        AccountStatus status,
        Long version
    ) {
    }

    /** Move-in-tree request — FR-223 re-parent, dedicated endpoint so it always
     *  goes through {@code NestedSetService.moveSubtree} explicitly. */
    public record MoveAccountRequest(
        String newParentId,
        @NotNull Long version
    ) {
    }

    public record AccountResponse(
        String id,
        String companyId,
        String code,
        String name,
        AccountType type,
        String parentId,
        AccountPostingType postingType,
        String currency,
        AccountStatus status,
        int lft,
        int rgt,
        int depth,
        long openingDebitAmount,
        long openingCreditAmount,
        Instant openingDate,
        String openingLocationId,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {
    }

    /** FR-227 — CSV import body (columns: code,name,type,parent_code,posting_type,currency). */
    public record CoaImportRequest(
        @NotBlank @Size(min = 26, max = 26) String companyId,
        @NotBlank String csv,
        boolean dryRun
    ) {
    }

    public record CoaImportRowResult(
        int lineNumber,
        String code,
        boolean accepted,
        String reason
    ) {
    }

    public record CoaImportResponse(
        int accepted,
        int skipped,
        List<CoaImportRowResult> rows
    ) {
    }
}

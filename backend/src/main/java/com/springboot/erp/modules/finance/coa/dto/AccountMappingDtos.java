package com.springboot.erp.modules.finance.coa.dto;

import com.springboot.erp.modules.finance.coa.domain.AccountModule;
import com.springboot.erp.modules.finance.coa.domain.AccountPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * Wire DTOs for module/purpose -> GL account mappings (reference
 * {@code app.finance.schemas} AccountMappingUpsertRequest / AccountMappingResponse /
 * CoaMappingStatusResponse). Records only, per ARCHITECTURE.md §2.
 */
public final class AccountMappingDtos {

    private AccountMappingDtos() {
    }

    /** POST /api/finance/account-mappings body — create or replace one mapping. */
    public record AccountMappingUpsertRequest(
        @NotBlank @Size(min = 26, max = 26) String companyId,
        @NotNull AccountModule module,
        @NotNull AccountPurpose purpose,
        @NotBlank @Size(min = 26, max = 26) String accountId
    ) {
    }

    public record AccountMappingResponse(
        String id,
        String companyId,
        AccountModule module,
        AccountPurpose purpose,
        String accountId,
        String accountCode,
        String accountName,
        long version,
        Instant updatedAt
    ) {
    }

    /** Per-module readiness — which (module, purpose) slots are configured vs. missing. */
    public record CoaMappingStatusResponse(
        AccountModule module,
        List<AccountPurpose> configured,
        List<AccountPurpose> missing,
        boolean ready
    ) {
    }

    /** Upsert response — the mapping plus recomputed per-module readiness. */
    public record AccountMappingMutationResponse(
        AccountMappingResponse mapping,
        List<CoaMappingStatusResponse> status
    ) {
    }

    public record AccountMappingDeleteResponse(
        List<CoaMappingStatusResponse> status
    ) {
    }
}

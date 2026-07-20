package com.springboot.erp.modules.inventory.stock.dto;

import com.springboot.erp.modules.inventory.stock.domain.MovementType;
import com.springboot.erp.modules.inventory.stock.domain.SourceDocType;
import com.springboot.erp.modules.inventory.stock.domain.StockStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Wire DTOs for the append-only {@link com.springboot.erp.modules.inventory.stock.domain.StockLedger}
 * (records — ARCHITECTURE.md §2). The ledger is append-only, so there is a create
 * (post a movement) and a read shape but no update/delete DTO. All ids are ULIDs.
 */
public final class StockLedgerDtos {

    private StockLedgerDtos() {
    }

    /** POST /api/inventory/ledger body — append one movement row. */
    public record LedgerPostRequest(
        @NotBlank @Size(min = 26, max = 26) String productId,
        @Size(min = 26, max = 26) String variantId,
        @NotBlank @Size(min = 26, max = 26) String locationId,
        StockStatus status,
        @NotNull MovementType movementType,
        @NotNull BigDecimal qtySigned,
        long unitCostAmount,
        @Size(min = 3, max = 3) String unitCostCurrency,
        long valueDeltaAmount,
        @NotNull SourceDocType sourceDocType,
        @NotBlank @Size(max = 26) String sourceDocId,
        @Size(min = 26, max = 26) String batchId,
        Instant occurredAt,
        Instant expiryDate,
        @Size(max = 500) String notes
    ) {
    }

    /** Ledger read shape. {@code id} is the ULID public id. */
    public record LedgerEntryResponse(
        String id,
        Instant occurredAt,
        String productId,
        String variantId,
        String locationId,
        StockStatus status,
        MovementType movementType,
        BigDecimal qtySigned,
        long unitCostAmount,
        String unitCostCurrency,
        long valueDeltaAmount,
        SourceDocType sourceDocType,
        String sourceDocId,
        String batchId,
        String notes
    ) {
    }
}

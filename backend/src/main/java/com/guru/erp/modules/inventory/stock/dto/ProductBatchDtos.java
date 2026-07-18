package com.guru.erp.modules.inventory.stock.dto;

import com.guru.erp.modules.inventory.stock.domain.BatchBarcodeFormat;
import com.guru.erp.modules.inventory.stock.domain.BatchBarcodeSource;
import com.guru.erp.modules.inventory.stock.domain.SourceDocType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Wire DTOs for {@link com.guru.erp.modules.inventory.stock.domain.ProductBatch}
 * (records). Standard create / update (PATCH) / read shapes; ids are ULIDs.
 */
public final class ProductBatchDtos {

    private ProductBatchDtos() {
    }

    /** POST /api/inventory/batches body. */
    public record BatchCreateRequest(
        @NotBlank @Size(min = 26, max = 26) String productId,
        @Size(min = 26, max = 26) String variantId,
        @NotNull SourceDocType sourceDocType,
        @NotBlank @Size(max = 50) String sourceDocId,
        @Size(max = 100) String batchNo,
        @Size(max = 64) String barcode,
        @NotNull BatchBarcodeFormat barcodeFormat,
        BatchBarcodeSource barcodeSource,
        BigDecimal qtyPerScan,
        long grnCostAmount,
        @Size(min = 3, max = 3) String grnCostCurrency,
        Long mrpAmount,
        @Size(min = 3, max = 3) String mrpCurrency,
        Long sellPriceAmount,
        @Size(min = 3, max = 3) String sellPriceCurrency,
        BigDecimal qtyReceived,
        Instant manufactureDate,
        Instant expiryDate
    ) {
    }

    /** PATCH /api/inventory/batches/{publicId} body — present fields applied. */
    public record BatchUpdateRequest(
        @Size(max = 100) String batchNo,
        @Size(max = 64) String barcode,
        BatchBarcodeFormat barcodeFormat,
        BatchBarcodeSource barcodeSource,
        BigDecimal qtyPerScan,
        Long mrpAmount,
        @Size(min = 3, max = 3) String mrpCurrency,
        Long sellPriceAmount,
        @Size(min = 3, max = 3) String sellPriceCurrency,
        BigDecimal qtyReceived,
        Instant manufactureDate,
        Instant expiryDate,
        Long version
    ) {
    }

    public record BatchResponse(
        String id,
        String productId,
        String variantId,
        SourceDocType sourceDocType,
        String sourceDocId,
        String batchNo,
        String barcode,
        BatchBarcodeFormat barcodeFormat,
        BatchBarcodeSource barcodeSource,
        BigDecimal qtyPerScan,
        long grnCostAmount,
        String grnCostCurrency,
        Long mrpAmount,
        String mrpCurrency,
        Long sellPriceAmount,
        String sellPriceCurrency,
        BigDecimal qtyReceived,
        Instant manufactureDate,
        Instant expiryDate,
        long version
    ) {
    }
}

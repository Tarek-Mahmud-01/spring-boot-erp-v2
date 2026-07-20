package com.springboot.erp.modules.product.catalog.dto;

/** Response for the CR-001 (FR-284) variable-measure barcode resolution endpoint. */
public final class WeighedDtos {

    private WeighedDtos() {
    }

    public record VariableMeasureResolveResponse(
        String productId,
        String sku,
        String name,
        String barcode,
        String plu,
        long unitPriceAmount,
        String currency,
        String taxCodeId,
        boolean restrictionAge18,
        boolean restrictionAge21,
        boolean restrictionControlledDisplay
    ) {
    }
}

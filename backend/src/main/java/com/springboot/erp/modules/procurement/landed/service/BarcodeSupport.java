package com.springboot.erp.modules.procurement.landed.service;

import com.springboot.erp.modules.procurement.landed.domain.BarcodeFormat;
import com.springboot.erp.modules.procurement.landed.domain.UnitBarcode;
import com.springboot.erp.modules.procurement.landed.domain.UnitBarcodeItem;
import com.springboot.erp.modules.procurement.landed.dto.UnitBarcodeDtos.UnitBarcodeItemRequest;
import java.math.BigDecimal;
import java.util.List;

/**
 * Shared barcode helpers used by the unit-barcode and bundle command services — kept in one place so
 * the format-detection, EAN-13 check-digit, and bundle-item mapping rules can never diverge between
 * the two write paths.
 */
final class BarcodeSupport {

    private BarcodeSupport() {
    }

    /** Auto-detect EAN-13 / UPC-A by length; everything else is CODE128 (reference detect). */
    static BarcodeFormat detectFormat(String barcode) {
        if (barcode.matches("\\d{13}")) {
            return BarcodeFormat.EAN13;
        }
        if (barcode.matches("\\d{12}")) {
            return BarcodeFormat.UPCA;
        }
        return BarcodeFormat.CODE128;
    }

    /** EAN-13 check digit for a 12-digit prefix. */
    static String ean13CheckDigit(String digits12) {
        int total = 0;
        for (int i = 0; i < digits12.length(); i++) {
            int d = digits12.charAt(i) - '0';
            total += (i % 2 == 0) ? d : d * 3;
        }
        return String.valueOf((10 - (total % 10)) % 10);
    }

    /** Attach bundle-component rows to a header from the request items. */
    static void applyItems(UnitBarcode ub, List<UnitBarcodeItemRequest> items) {
        if (items == null) {
            return;
        }
        for (UnitBarcodeItemRequest in : items) {
            UnitBarcodeItem item = new UnitBarcodeItem();
            item.setProductId(in.productId());
            item.setVariantId(in.variantId());
            item.setGrnLineId(in.grnLineId());
            item.setQty(in.qty() == null ? BigDecimal.ONE : in.qty());
            ub.addItem(item);
        }
    }
}

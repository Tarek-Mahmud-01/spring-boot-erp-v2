package com.guru.erp.modules.product.catalog.domain;

import java.util.regex.Pattern;

/**
 * FR-057 / FR-284 barcode format validation + GS1 in-store variable-measure
 * decoding, ported from the reference {@code products/views.py}. Pure functions,
 * no persistence — the service calls {@link #validate} before saving a barcode
 * and {@link #decodeVariableMeasure} to resolve a scale-printed price-embedded
 * scan.
 */
public final class BarcodeValidator {

    private static final Pattern DIGIT_ONLY = Pattern.compile("^\\d+$");
    // CODE128: up to 48 printable ASCII characters.
    private static final Pattern CODE128 = Pattern.compile("^[\\x20-\\x7E]{1,48}$");

    /** GS1 in-store variable-measure flag prefix (CR-001 §1). */
    public static final String VARIABLE_MEASURE_PREFIX = "21";
    private static final int VM_PLU_WIDTH = 5;
    private static final int VM_PRICE_WIDTH = 5;

    private BarcodeValidator() {
    }

    /** Decoded GS1 in-store price-embedded symbol (FR-284). */
    public record VariableMeasure(String plu, long priceMinorUnits) {
    }

    /**
     * FR-057 — validate length/character-set per declared format and return the
     * cleaned (trimmed) barcode. Returns {@code null} when validation fails so the
     * caller can raise the format-specific BARCODE_INVALID error.
     */
    public static String validate(String barcode, BarcodeFormat fmt) {
        String cleaned = barcode == null ? "" : barcode.strip();
        return switch (fmt) {
            case EAN13 -> ean13Checksum(cleaned) ? cleaned : null;
            case UPCA -> upcaChecksum(cleaned) ? cleaned : null;
            case CODE128 -> CODE128.matcher(cleaned).matches() ? cleaned : null;
            case OTHER -> (!cleaned.isEmpty() && cleaned.length() <= 50) ? cleaned : null;
            // WEIGHTED / PLU are resolved through other flows, not stored as plain barcodes here.
            case WEIGHTED, PLU -> (!cleaned.isEmpty() && cleaned.length() <= 64) ? cleaned : null;
        };
    }

    public static boolean ean13Checksum(String barcode) {
        if (barcode.length() != 13 || !DIGIT_ONLY.matcher(barcode).matches()) {
            return false;
        }
        int odd = 0;
        int even = 0;
        for (int i = 0; i < 12; i++) {
            int d = barcode.charAt(i) - '0';
            if (i % 2 == 0) {
                odd += d;
            } else {
                even += d;
            }
        }
        int check = (10 - ((odd + even * 3) % 10)) % 10;
        return check == (barcode.charAt(12) - '0');
    }

    public static boolean upcaChecksum(String barcode) {
        if (barcode.length() != 12 || !DIGIT_ONLY.matcher(barcode).matches()) {
            return false;
        }
        int odd = 0;
        int even = 0;
        for (int i = 0; i < 11; i++) {
            int d = barcode.charAt(i) - '0';
            if (i % 2 == 0) {
                odd += d;
            } else {
                even += d;
            }
        }
        int check = (10 - ((odd * 3 + even) % 10)) % 10;
        return check == (barcode.charAt(11) - '0');
    }

    public static boolean isVariableMeasure(String barcode) {
        String cleaned = barcode == null ? "" : barcode.strip();
        return cleaned.length() == 13 && cleaned.startsWith(VARIABLE_MEASURE_PREFIX);
    }

    /**
     * FR-284 — decode a GS1 in-store price-embedded EAN-13 (prefix "21"):
     * positions 3-7 = PLU, positions 8-12 = total price in minor units, position
     * 13 = EAN-13 checksum. Returns {@code null} when the code is not 13 digits,
     * fails the checksum, or lacks the "21" prefix.
     */
    public static VariableMeasure decodeVariableMeasure(String barcode) {
        String cleaned = barcode == null ? "" : barcode.strip();
        if (!isVariableMeasure(cleaned) || !ean13Checksum(cleaned)) {
            return null;
        }
        int pluStart = VARIABLE_MEASURE_PREFIX.length();
        int pluEnd = pluStart + VM_PLU_WIDTH;
        int priceEnd = pluEnd + VM_PRICE_WIDTH;
        String plu = cleaned.substring(pluStart, pluEnd);
        long price = Long.parseLong(cleaned.substring(pluEnd, priceEnd));
        return new VariableMeasure(plu, price);
    }
}

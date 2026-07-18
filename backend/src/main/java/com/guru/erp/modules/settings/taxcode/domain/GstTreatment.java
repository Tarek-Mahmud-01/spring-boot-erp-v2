package com.guru.erp.modules.settings.taxcode.domain;

/**
 * US-AU-005 — which BAS sales box a tax code's sales feed (ENT-007).
 *
 * <ul>
 *   <li>{@code STANDARD} — contributes to G1 and 1A.</li>
 *   <li>{@code GST_FREE} — basic food/health/education; counts toward G1 but
 *       breaks out into G3 with zero GST.</li>
 *   <li>{@code EXPORT} — GST-free exports; counts toward G1, breaks out into G2.</li>
 *   <li>{@code INPUT_TAXED} — financial supplies/residential rent; no GST and no
 *       input tax credit. Kept distinct from {@code GST_FREE} for BAS adjustment
 *       worksheets.</li>
 * </ul>
 */
public enum GstTreatment {
    STANDARD,
    GST_FREE,
    EXPORT,
    INPUT_TAXED
}

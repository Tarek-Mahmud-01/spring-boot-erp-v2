package com.springboot.erp.modules.settings.currency.service;

import org.springframework.stereotype.Component;

/**
 * Cross-module reference checks that gate currency deletion and default swaps.
 *
 * <p>In the reference app these probe finance ({@code Account}, {@code JournalLine},
 * {@code JournalEntry}), procurement ({@code PurchaseOrder}, {@code SupplierBill},
 * {@code SupplierPayment}, {@code AmountReceived}, {@code SupplierReturn}),
 * companies ({@code Company.base_currency}) and exchange-rate tables — none of
 * which exist in Guru ERP v2 yet. This default implementation therefore reports
 * "no references / no history"; replace it (or wire additional
 * {@code @Component} probes) when those modules are ported so the delete and
 * set-default guards regain their teeth.
 *
 * <p>Isolating the checks here keeps {@link CurrencyService} free of hard
 * compile-time deps on modules that don't exist, exactly as the reference used
 * lazy imports at the call edge.
 */
@Component
public class CurrencyReferenceGuard {

    /**
     * True when the ISO code is still referenced by any account, journal line,
     * exchange rate, or a company's base currency. Blocks hard/soft delete
     * (reference bug QA BUG-22). Currency codes carry no FK, so this
     * service-layer probe is the only backstop.
     */
    public boolean isInUse(String code) {
        return false;
    }

    /**
     * True when at least one GL-or-supply transactional row exists. Once true,
     * switching the base (default) currency is forbidden because historical
     * amounts were booked against the old default with no safe retroactive
     * re-conversion.
     */
    public boolean hasTransactionalHistory() {
        return false;
    }
}

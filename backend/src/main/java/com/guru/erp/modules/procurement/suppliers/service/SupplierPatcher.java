package com.guru.erp.modules.procurement.suppliers.service;

import com.guru.erp.modules.procurement.suppliers.domain.Supplier;
import com.guru.erp.modules.procurement.suppliers.dto.SupplierDtos.SupplierUpdateRequest;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import com.guru.erp.platform.money.Money;

/**
 * Applies a {@link SupplierUpdateRequest} onto a {@link Supplier} — every field is optional, so a
 * {@code null} leaves the current value unchanged (reference {@code update_supplier} PATCH
 * semantics). Extracted from {@code SupplierCommandService} to keep that service under its size cap.
 * The credit-limit / opening-balance amount + currency pair is merged so a partial change to one
 * side preserves the other on the embedded {@link Money}.
 */
final class SupplierPatcher {

    private SupplierPatcher() {
    }

    static void apply(Supplier s, SupplierUpdateRequest req) {
        if (req.name() != null) {
            String stripped = req.name().strip();
            if (stripped.isEmpty()) {
                throw new DomainException(ErrorCode.VALIDATION_FAILED, "Name cannot be blank.");
            }
            s.setName(stripped);
        }
        if (req.type() != null) {
            s.setType(req.type());
        }
        if (req.locationId() != null) {
            s.setLocationId(req.locationId());
        }
        if (req.contact() != null) {
            s.setContact(req.contact());
        }
        if (req.address() != null) {
            s.setAddress(req.address());
        }
        if (req.paymentTerms() != null) {
            s.setPaymentTerms(req.paymentTerms());
        }
        if (req.defaultCurrency() != null) {
            s.setDefaultCurrency(req.defaultCurrency().toUpperCase());
        }
        if (req.taxRegistrationNo() != null) {
            s.setTaxRegistrationNo(req.taxRegistrationNo());
        }
        if (req.abn() != null) {
            s.setAbn(blankToNull(req.abn()));
        }
        if (req.bankDetails() != null) {
            s.setBankDetails(req.bankDetails());
        }
        if (req.creditLimitAmount() != null || req.creditLimitCurrency() != null) {
            long amt = req.creditLimitAmount() != null
                ? req.creditLimitAmount() : s.getCreditLimit().amountMinor();
            String ccy = req.creditLimitCurrency() != null
                ? req.creditLimitCurrency().toUpperCase() : s.getCreditLimit().currency();
            s.setCreditLimit(Money.ofMinor(amt, ccy));
        }
        if (req.openingBalanceAmount() != null || req.openingBalanceCurrency() != null) {
            long amt = req.openingBalanceAmount() != null
                ? req.openingBalanceAmount() : s.getOpeningBalance().amountMinor();
            String ccy = req.openingBalanceCurrency() != null
                ? req.openingBalanceCurrency().toUpperCase() : s.getOpeningBalance().currency();
            s.setOpeningBalance(Money.ofMinor(amt, ccy));
        }
        if (req.openingBalanceSide() != null) {
            s.setOpeningBalanceSide(req.openingBalanceSide());
        }
        if (req.openingBalanceDate() != null) {
            s.setOpeningBalanceDate(req.openingBalanceDate());
        }
        if (req.openingBalanceExchangeRate() != null) {
            s.setOpeningBalanceExchangeRate(req.openingBalanceExchangeRate());
        }
        if (req.openingBalanceAccountId() != null) {
            s.setOpeningBalanceAccountId(req.openingBalanceAccountId());
        }
    }

    private static String blankToNull(String v) {
        return v == null || v.strip().isEmpty() ? null : v.strip();
    }
}

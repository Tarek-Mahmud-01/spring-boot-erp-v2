package com.springboot.erp.modules.reporting.posloyalty.controller;

import com.springboot.erp.modules.reporting.posloyalty.dto.LoyaltyReportDtos.CustomersWithConsentResponse;
import com.springboot.erp.modules.reporting.posloyalty.dto.LoyaltyReportDtos.LoyaltyLedgerResponse;
import com.springboot.erp.modules.reporting.posloyalty.service.CustomerConsentQueryService;
import com.springboot.erp.modules.reporting.posloyalty.service.LoyaltyLedgerQueryService;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRM &amp; Loyalty reports (reference {@code app.reports.views.loyalty}) — RPT-022 Loyalty Ledger,
 * RPT-021 Customer List with Consent. Thin controller: every rule lives in the query services.
 * Read-only — gated by {@code reporting.loyalty.read}.
 */
@RestController
@RequestMapping("/api/reports/loyalty")
public class LoyaltyReportController {

    private final LoyaltyLedgerQueryService loyaltyLedger;
    private final CustomerConsentQueryService customersWithConsent;

    public LoyaltyReportController(LoyaltyLedgerQueryService loyaltyLedger,
                                   CustomerConsentQueryService customersWithConsent) {
        this.loyaltyLedger = loyaltyLedger;
        this.customersWithConsent = customersWithConsent;
    }

    /** RPT-022 — per-customer point movements with a true running balance. */
    @GetMapping("/ledger")
    @PreAuthorize("hasAuthority('reporting.loyalty.read')")
    public LoyaltyLedgerResponse ledger(@RequestParam String companyId,
                                        @RequestParam(required = false) String customerId,
                                        @RequestParam(required = false) LocalDate fromDate,
                                        @RequestParam(required = false) LocalDate toDate,
                                        @RequestParam(required = false) String kind,
                                        @PageableDefault(size = 50) Pageable pageable) {
        return loyaltyLedger.loyaltyLedger(companyId, customerId, fromDate, toDate, kind, pageable);
    }

    /** RPT-021 — consent-aware marketing extract with segment codes and rolling 12m spend. */
    @GetMapping("/customers-with-consent")
    @PreAuthorize("hasAuthority('reporting.loyalty.read')")
    public CustomersWithConsentResponse customersWithConsent(@RequestParam String companyId,
                                                             @RequestParam(required = false) String consent,
                                                             @RequestParam(required = false) String search,
                                                             @RequestParam(defaultValue = "true") boolean activeOnly,
                                                             @PageableDefault(size = 50) Pageable pageable) {
        return customersWithConsent.customersWithConsent(companyId, consent, search, activeOnly, pageable);
    }
}

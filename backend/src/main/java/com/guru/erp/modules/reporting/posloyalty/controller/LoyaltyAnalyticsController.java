package com.guru.erp.modules.reporting.posloyalty.controller;

import com.guru.erp.modules.reporting.posloyalty.dto.LoyaltyAnalyticsDtos.CustomerRfmResponse;
import com.guru.erp.modules.reporting.posloyalty.dto.LoyaltyAnalyticsDtos.LoyaltyLiabilityResponse;
import com.guru.erp.modules.reporting.posloyalty.dto.LoyaltyAnalyticsDtos.PointsExpiryResponse;
import com.guru.erp.modules.reporting.posloyalty.dto.LoyaltyAnalyticsDtos.TierDistributionResponse;
import com.guru.erp.modules.reporting.posloyalty.service.CustomerRfmQueryService;
import com.guru.erp.modules.reporting.posloyalty.service.LoyaltyLiabilityQueryService;
import com.guru.erp.modules.reporting.posloyalty.service.PointsExpiryQueryService;
import com.guru.erp.modules.reporting.posloyalty.service.TierDistributionQueryService;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Owner-facing loyalty analytics reports (reference {@code app.reports.views.loyalty_analytics}) —
 * RPT-028 Loyalty Liability &amp; Breakage, RPT-029 Points Expiry Forecast, RPT-030 Tier
 * Distribution &amp; Migration, RPT-031 Customer RFM/LTV. Thin controller: every rule lives in the
 * query services. Read-only — gated by {@code reporting.loyalty.read} (same permission as the
 * ledger/consent reports; both surface the same loyalty program data to the same audience).
 */
@RestController
@RequestMapping("/api/reports/loyalty-analytics")
public class LoyaltyAnalyticsController {

    private final LoyaltyLiabilityQueryService loyaltyLiability;
    private final PointsExpiryQueryService pointsExpiry;
    private final TierDistributionQueryService tierDistribution;
    private final CustomerRfmQueryService customerRfm;

    public LoyaltyAnalyticsController(LoyaltyLiabilityQueryService loyaltyLiability,
                                      PointsExpiryQueryService pointsExpiry,
                                      TierDistributionQueryService tierDistribution,
                                      CustomerRfmQueryService customerRfm) {
        this.loyaltyLiability = loyaltyLiability;
        this.pointsExpiry = pointsExpiry;
        this.tierDistribution = tierDistribution;
        this.customerRfm = customerRfm;
    }

    /** RPT-028 — points balance/liability + breakage % per member, company-wide breakage summary. */
    @GetMapping("/liability")
    @PreAuthorize("hasAuthority('reporting.loyalty.read')")
    public LoyaltyLiabilityResponse liability(@RequestParam String companyId,
                                              @RequestParam(required = false) String tierId,
                                              @RequestParam(defaultValue = "false") boolean nonZeroOnly,
                                              @PageableDefault(size = 50) Pageable pageable) {
        return loyaltyLiability.loyaltyLiability(companyId, tierId, nonZeroOnly, pageable);
    }

    /** RPT-029 — FIFO earn-lot expiry forecast, bucketed into 30/60/90-day windows + overdue. */
    @GetMapping("/points-expiry")
    @PreAuthorize("hasAuthority('reporting.loyalty.read')")
    public PointsExpiryResponse pointsExpiry(@RequestParam String companyId,
                                             @RequestParam(required = false) String tierId,
                                             @RequestParam(required = false) String withinDays,
                                             @PageableDefault(size = 50) Pageable pageable) {
        return pointsExpiry.pointsExpiry(companyId, tierId, withinDays, pageable);
    }

    /** RPT-030 — active-member tier distribution + due-upgrade/downgrade migration flags. */
    @GetMapping("/tier-distribution")
    @PreAuthorize("hasAuthority('reporting.loyalty.read')")
    public TierDistributionResponse tierDistribution(@RequestParam String companyId,
                                                     @PageableDefault(size = 50) Pageable pageable) {
        return tierDistribution.tierDistribution(companyId, pageable);
    }

    /** RPT-031 — population-quintile Recency/Frequency/Monetary scoring + segment classification. */
    @GetMapping("/customer-rfm")
    @PreAuthorize("hasAuthority('reporting.loyalty.read')")
    public CustomerRfmResponse customerRfm(@RequestParam String companyId,
                                           @RequestParam(required = false) LocalDate fromDate,
                                           @RequestParam(required = false) LocalDate toDate,
                                           @RequestParam(required = false) String rfmSegment,
                                           @RequestParam(required = false) String tierId,
                                           @PageableDefault(size = 50) Pageable pageable) {
        return customerRfm.customerRfm(companyId, fromDate, toDate, rfmSegment, tierId, pageable);
    }
}

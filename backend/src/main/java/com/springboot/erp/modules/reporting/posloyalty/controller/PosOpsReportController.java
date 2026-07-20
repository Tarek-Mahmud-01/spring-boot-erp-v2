package com.springboot.erp.modules.reporting.posloyalty.controller;

import com.springboot.erp.modules.reporting.posloyalty.dto.PosOpsReportDtos.AbandonedCartResponse;
import com.springboot.erp.modules.reporting.posloyalty.dto.PosOpsReportDtos.AgeRefusalResponse;
import com.springboot.erp.modules.reporting.posloyalty.dto.PosOpsReportDtos.DailySalesResponse;
import com.springboot.erp.modules.reporting.posloyalty.dto.PosOpsReportDtos.DiscountResponse;
import com.springboot.erp.modules.reporting.posloyalty.dto.PosOpsReportDtos.NewVsReturningResponse;
import com.springboot.erp.modules.reporting.posloyalty.dto.PosOpsReportDtos.TillVarianceResponse;
import com.springboot.erp.modules.reporting.posloyalty.service.AbandonedCartQueryService;
import com.springboot.erp.modules.reporting.posloyalty.service.AgeRefusalQueryService;
import com.springboot.erp.modules.reporting.posloyalty.service.DailySalesQueryService;
import com.springboot.erp.modules.reporting.posloyalty.service.DiscountUsageQueryService;
import com.springboot.erp.modules.reporting.posloyalty.service.NewVsReturningQueryService;
import com.springboot.erp.modules.reporting.posloyalty.service.TillVarianceQueryService;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * POS operational reports (reference {@code app.reports.views.pos_ops}) — RPT-017 Discount Usage,
 * RPT-018 POS Daily Sales Summary, RPT-019 Till Variance, RPT-AU-004 Age Verification Refusals,
 * RPT-032 New vs Returning Customers, RPT-033 Suspended/Abandoned Carts. Thin controller: every
 * rule lives in the query services. Read-only — gated by {@code reporting.pos.read}.
 */
@RestController
@RequestMapping("/api/reports/pos")
public class PosOpsReportController {

    private final DiscountUsageQueryService discountUsage;
    private final DailySalesQueryService dailySales;
    private final TillVarianceQueryService tillVariance;
    private final AgeRefusalQueryService ageRefusals;
    private final NewVsReturningQueryService newVsReturning;
    private final AbandonedCartQueryService abandonedCarts;

    public PosOpsReportController(DiscountUsageQueryService discountUsage, DailySalesQueryService dailySales,
                                  TillVarianceQueryService tillVariance, AgeRefusalQueryService ageRefusals,
                                  NewVsReturningQueryService newVsReturning,
                                  AbandonedCartQueryService abandonedCarts) {
        this.discountUsage = discountUsage;
        this.dailySales = dailySales;
        this.tillVariance = tillVariance;
        this.ageRefusals = ageRefusals;
        this.newVsReturning = newVsReturning;
        this.abandonedCarts = abandonedCarts;
    }

    /** RPT-017 — manual + promotional discount usage, one row per discount event/line. */
    @GetMapping("/discount-usage")
    @PreAuthorize("hasAuthority('reporting.pos.read')")
    public DiscountResponse discountUsage(@RequestParam String companyId,
                                          @RequestParam(required = false) String locationId,
                                          @RequestParam(required = false) String cashierId,
                                          @RequestParam(required = false) String reason,
                                          @RequestParam(required = false) String kind,
                                          @RequestParam(required = false) LocalDate fromDate,
                                          @RequestParam(required = false) LocalDate toDate,
                                          @PageableDefault(size = 50) Pageable pageable) {
        return discountUsage.discountUsage(companyId, locationId, cashierId, reason, kind, fromDate, toDate, pageable);
    }

    /** RPT-018 — per-day, per-location gross/refund/tax/tender-split summary. */
    @GetMapping("/daily-sales")
    @PreAuthorize("hasAuthority('reporting.pos.read')")
    public DailySalesResponse dailySales(@RequestParam String companyId,
                                         @RequestParam(required = false) String locationId,
                                         @RequestParam(required = false) LocalDate fromDate,
                                         @RequestParam(required = false) LocalDate toDate,
                                         @PageableDefault(size = 50) Pageable pageable) {
        return dailySales.dailySales(companyId, locationId, fromDate, toDate, pageable);
    }

    /** RPT-019 — till cash-count variance, optionally filtered to over-threshold sessions. */
    @GetMapping("/till-variance")
    @PreAuthorize("hasAuthority('reporting.pos.read')")
    public TillVarianceResponse tillVariance(@RequestParam String companyId,
                                             @RequestParam(required = false) String locationId,
                                             @RequestParam(required = false) String cashierId,
                                             @RequestParam(defaultValue = "0") long overThreshold,
                                             @RequestParam(required = false) LocalDate fromDate,
                                             @RequestParam(required = false) LocalDate toDate,
                                             @PageableDefault(size = 50) Pageable pageable) {
        return tillVariance.tillVariance(companyId, locationId, cashierId, overThreshold, fromDate, toDate, pageable);
    }

    /** RPT-AU-004 — age-verification refusal timeline. */
    @GetMapping("/age-refusals")
    @PreAuthorize("hasAuthority('reporting.pos.read')")
    public AgeRefusalResponse ageRefusals(@RequestParam String companyId,
                                          @RequestParam(required = false) String cashierId,
                                          @RequestParam(required = false) String locationId,
                                          @RequestParam(required = false) LocalDate fromDate,
                                          @RequestParam(required = false) LocalDate toDate,
                                          @PageableDefault(size = 50) Pageable pageable) {
        return ageRefusals.ageRefusals(companyId, cashierId, locationId, fromDate, toDate, pageable);
    }

    /** RPT-032 — daily new-vs-returning customer counts and revenue split. */
    @GetMapping("/new-vs-returning")
    @PreAuthorize("hasAuthority('reporting.pos.read')")
    public NewVsReturningResponse newVsReturning(@RequestParam String companyId,
                                                 @RequestParam(required = false) String locationId,
                                                 @RequestParam(required = false) LocalDate fromDate,
                                                 @RequestParam(required = false) LocalDate toDate,
                                                 @PageableDefault(size = 50) Pageable pageable) {
        return newVsReturning.newVsReturning(companyId, locationId, fromDate, toDate, pageable);
    }

    /** RPT-033 — parked-but-not-resumed carts, ACTIVE or EXPIRED. */
    @GetMapping("/abandoned-carts")
    @PreAuthorize("hasAuthority('reporting.pos.read')")
    public AbandonedCartResponse abandonedCarts(@RequestParam String companyId,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) LocalDate fromDate,
                                                @RequestParam(required = false) LocalDate toDate,
                                                @PageableDefault(size = 50) Pageable pageable) {
        return abandonedCarts.abandonedCarts(companyId, status, fromDate, toDate, pageable);
    }
}

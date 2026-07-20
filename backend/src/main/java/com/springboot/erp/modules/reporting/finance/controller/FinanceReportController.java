package com.springboot.erp.modules.reporting.finance.controller;

import com.springboot.erp.modules.reporting.finance.dto.FinanceReportDtos.AccountLedgerRow;
import com.springboot.erp.modules.reporting.finance.dto.FinanceReportDtos.AccountLedgerSummary;
import com.springboot.erp.modules.reporting.finance.dto.FinanceReportDtos.PaymentsReceiptsRow;
import com.springboot.erp.modules.reporting.finance.dto.FinanceReportDtos.PaymentsReceiptsSummary;
import com.springboot.erp.modules.reporting.finance.dto.FinanceReportDtos.TrialBalanceRow;
import com.springboot.erp.modules.reporting.finance.dto.FinanceReportDtos.TrialBalanceSummary;
import com.springboot.erp.modules.reporting.finance.service.AccountLedgerReportService;
import com.springboot.erp.modules.reporting.finance.service.PaymentsReceiptsReportService;
import com.springboot.erp.modules.reporting.finance.service.TrialBalanceReportService;
import com.springboot.erp.platform.web.PageResponse;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REPORTING "finance" sub-slice, part 1 — Account Ledger / Trial Balance / Payments &amp; Receipts
 * (reference {@code app.reports.views.finance}). Thin controller: delegates to a query service per
 * report that builds the QueryDSL aggregation. See {@link FinanceStatementController} for
 * Cash Book / Income Statement / Balance Sheet. No mutations, no audit rows.
 */
@RestController
@RequestMapping("/api/reports/finance")
public class FinanceReportController {

    private final AccountLedgerReportService accountLedger;
    private final TrialBalanceReportService trialBalance;
    private final PaymentsReceiptsReportService paymentsReceipts;

    public FinanceReportController(AccountLedgerReportService accountLedger, TrialBalanceReportService trialBalance,
                                   PaymentsReceiptsReportService paymentsReceipts) {
        this.accountLedger = accountLedger;
        this.trialBalance = trialBalance;
        this.paymentsReceipts = paymentsReceipts;
    }

    /** RPT-E009-ACC-LEDGER — chronological journal lines for one GL account with a running balance. */
    @GetMapping("/account-ledger")
    @PreAuthorize("hasAuthority('reporting.finance.read')")
    public PageResponse<AccountLedgerRow> accountLedger(
            @RequestParam String companyId, @RequestParam String accountId,
            @RequestParam(required = false) LocalDate fromDate, @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String voucherType, @RequestParam(required = false) String locationId,
            @PageableDefault(size = 50) Pageable pageable) {
        return accountLedger.list(companyId, accountId, fromDate, toDate, voucherType, locationId, pageable);
    }

    @GetMapping("/account-ledger/summary")
    @PreAuthorize("hasAuthority('reporting.finance.read')")
    public AccountLedgerSummary accountLedgerSummary(
            @RequestParam String companyId, @RequestParam String accountId,
            @RequestParam(required = false) LocalDate fromDate, @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String voucherType, @RequestParam(required = false) String locationId) {
        return accountLedger.summary(companyId, accountId, fromDate, toDate, voucherType, locationId);
    }

    /** RPT-E009-TRIAL-BAL — debit/credit totals per account for the period. */
    @GetMapping("/trial-balance")
    @PreAuthorize("hasAuthority('reporting.finance.read')")
    public PageResponse<TrialBalanceRow> trialBalance(
            @RequestParam String companyId, @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate, @RequestParam(required = false) String locationId,
            @PageableDefault(size = 50) Pageable pageable) {
        return trialBalance.list(companyId, fromDate, toDate, locationId, pageable);
    }

    @GetMapping("/trial-balance/summary")
    @PreAuthorize("hasAuthority('reporting.finance.read')")
    public TrialBalanceSummary trialBalanceSummary(
            @RequestParam String companyId, @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate, @RequestParam(required = false) String locationId) {
        return trialBalance.summary(companyId, fromDate, toDate, locationId);
    }

    /** RPT-E009-PAY-RECEIPT — cash inflows/outflows across every cash/bank GL account. */
    @GetMapping("/payments-receipts")
    @PreAuthorize("hasAuthority('reporting.finance.read')")
    public PageResponse<PaymentsReceiptsRow> paymentsReceipts(
            @RequestParam String companyId, @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate, @RequestParam(required = false) String direction,
            @RequestParam(required = false) String locationId, @PageableDefault(size = 50) Pageable pageable) {
        return paymentsReceipts.list(companyId, fromDate, toDate, direction, locationId, pageable);
    }

    @GetMapping("/payments-receipts/summary")
    @PreAuthorize("hasAuthority('reporting.finance.read')")
    public PaymentsReceiptsSummary paymentsReceiptsSummary(
            @RequestParam String companyId, @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate, @RequestParam(required = false) String direction,
            @RequestParam(required = false) String locationId) {
        return paymentsReceipts.summary(companyId, fromDate, toDate, direction, locationId);
    }
}

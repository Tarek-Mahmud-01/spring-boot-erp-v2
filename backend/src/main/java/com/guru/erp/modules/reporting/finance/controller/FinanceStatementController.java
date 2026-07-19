package com.guru.erp.modules.reporting.finance.controller;

import com.guru.erp.modules.reporting.finance.dto.FinanceReportDtos.BalanceSheetResponse;
import com.guru.erp.modules.reporting.finance.dto.FinanceReportDtos.CashBookRow;
import com.guru.erp.modules.reporting.finance.dto.FinanceReportDtos.CashBookSummary;
import com.guru.erp.modules.reporting.finance.dto.FinanceReportDtos.IncomeStatementResponse;
import com.guru.erp.modules.reporting.finance.service.BalanceSheetReportService;
import com.guru.erp.modules.reporting.finance.service.CashBookReportService;
import com.guru.erp.modules.reporting.finance.service.IncomeStatementReportService;
import com.guru.erp.platform.web.PageResponse;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REPORTING "finance" sub-slice, part 2 — Cash Book / Income Statement / Balance Sheet (reference
 * {@code app.reports.views.finance}). See {@link FinanceReportController} for Account Ledger /
 * Trial Balance / Payments &amp; Receipts. No mutations, no audit rows.
 */
@RestController
@RequestMapping("/api/reports/finance")
public class FinanceStatementController {

    private final CashBookReportService cashBook;
    private final IncomeStatementReportService incomeStatement;
    private final BalanceSheetReportService balanceSheet;

    public FinanceStatementController(CashBookReportService cashBook, IncomeStatementReportService incomeStatement,
                                      BalanceSheetReportService balanceSheet) {
        this.cashBook = cashBook;
        this.incomeStatement = incomeStatement;
        this.balanceSheet = balanceSheet;
    }

    /** RPT-E009-CASH-BOOK — chronological cash/bank journal lines with a running balance. */
    @GetMapping("/cash-book")
    @PreAuthorize("hasAuthority('reporting.finance.read')")
    public PageResponse<CashBookRow> cashBook(
            @RequestParam String companyId, @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate, @RequestParam(required = false) String voucherType,
            @RequestParam(required = false) String locationId,
            @RequestParam(required = false) String paymentMethodId, @PageableDefault(size = 50) Pageable pageable) {
        return cashBook.list(companyId, fromDate, toDate, voucherType, locationId, pageable);
    }

    @GetMapping("/cash-book/summary")
    @PreAuthorize("hasAuthority('reporting.finance.read')")
    public CashBookSummary cashBookSummary(
            @RequestParam String companyId, @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate, @RequestParam(required = false) String voucherType,
            @RequestParam(required = false) String locationId,
            @RequestParam(required = false) String paymentMethodId) {
        return cashBook.summary(companyId, fromDate, toDate, voucherType, locationId);
    }

    /** RPT-E009-INCOME-STMT — revenue/expense aggregation for a period (single object, not paged). */
    @GetMapping("/income-statement")
    @PreAuthorize("hasAuthority('reporting.finance.read')")
    public IncomeStatementResponse incomeStatement(
            @RequestParam String companyId, @RequestParam LocalDate fromDate, @RequestParam LocalDate toDate,
            @RequestParam(required = false) String locationId) {
        return incomeStatement.run(companyId, fromDate, toDate, locationId);
    }

    /** RPT-E009-BAL-SHEET — Assets/Liabilities/Equity snapshot as of a cutoff date (single object). */
    @GetMapping("/balance-sheet")
    @PreAuthorize("hasAuthority('reporting.finance.read')")
    public BalanceSheetResponse balanceSheet(
            @RequestParam String companyId, @RequestParam LocalDate asOfDate,
            @RequestParam(required = false) String locationId) {
        return balanceSheet.run(companyId, asOfDate, locationId);
    }
}

package com.guru.erp.modules.reporting.finance.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Wire DTOs for the Finance reporting sub-slice (reference {@code app.reports.schemas.finance}).
 * Records only (ARCHITECTURE.md §2). Every money field is a plain {@code long} minor-units amount
 * paired with a sibling {@code currency} string — never a nested Money entity on the wire, mirroring
 * how every other ported slice shapes its response DTOs.
 *
 * <p>List-style reports (Account Ledger, Trial Balance, Payments &amp; Receipts, Cash Book) return
 * their rows via {@link com.guru.erp.platform.web.PageResponse} with a sibling {@code summary}
 * fetched separately by the controller; single-shape reports (Income Statement, Balance Sheet)
 * return a plain record.
 */
public final class FinanceReportDtos {

    private FinanceReportDtos() {
    }

    // -----------------------------------------------------------------
    // Account Ledger (RPT-E009-ACC-LEDGER)
    // -----------------------------------------------------------------

    public record AccountLedgerRow(
        String entryId,
        LocalDate entryDate,
        String voucherType,
        String voucherNumber,
        String lineId,
        String lineNarration,
        String entryNarration,
        String reference,
        long debit,
        long credit,
        long runningBalance,
        String currency,
        String holderType,
        String holderId
    ) {
    }

    public record AccountLedgerSummary(
        String accountId,
        String accountCode,
        String accountName,
        String accountType,
        String currency,
        long openingBalance,
        long totalDebit,
        long totalCredit,
        long closingBalance,
        long lineCount
    ) {
    }

    // -----------------------------------------------------------------
    // Trial Balance (RPT-E009-TRIAL-BAL)
    // -----------------------------------------------------------------

    public record TrialBalanceRow(
        String accountId,
        String accountCode,
        String accountName,
        String accountType,
        String currency,
        long totalDebit,
        long totalCredit,
        long balance
    ) {
    }

    public record TrialBalanceSummary(
        String companyId,
        String currency,
        long totalDebit,
        long totalCredit,
        boolean isBalanced
    ) {
    }

    // -----------------------------------------------------------------
    // Payments & Receipts (RPT-E009-PAY-RECEIPT)
    // -----------------------------------------------------------------

    public record PaymentsReceiptsRow(
        String entryId,
        LocalDate entryDate,
        String voucherType,
        String voucherNumber,
        String direction,
        String counterpartyType,
        String counterpartyId,
        String reference,
        String narration,
        long amount,
        String currency
    ) {
    }

    public record PaymentsReceiptsSummary(
        String companyId,
        String currency,
        long totalIn,
        long totalOut,
        long net,
        long lineCount
    ) {
    }

    // -----------------------------------------------------------------
    // Cash Book (RPT-E009-CASH-BOOK)
    // -----------------------------------------------------------------

    public record CashBookRow(
        String entryId,
        LocalDate entryDate,
        String voucherType,
        String voucherNumber,
        String lineId,
        String lineNarration,
        String entryNarration,
        String reference,
        String accountCode,
        String accountName,
        long debit,
        long credit,
        long runningBalance,
        String currency,
        String holderType,
        String holderId
    ) {
    }

    public record CashBookSummary(
        String companyId,
        String currency,
        long openingBalance,
        long totalDebit,
        long totalCredit,
        long closingBalance,
        long lineCount,
        long accountCount
    ) {
    }

    // -----------------------------------------------------------------
    // Income Statement (RPT-E009-INCOME-STMT) — no pagination, fixed shape
    // -----------------------------------------------------------------

    public record IncomeStatementLine(
        String accountId,
        String accountCode,
        String accountName,
        long amount,
        String currency
    ) {
    }

    public record IncomeStatementResponse(
        String companyId,
        String currency,
        LocalDate fromDate,
        LocalDate toDate,
        List<IncomeStatementLine> incomeLines,
        long totalIncome,
        List<IncomeStatementLine> expenseLines,
        long totalExpense,
        long netProfit
    ) {
    }

    // -----------------------------------------------------------------
    // Balance Sheet (RPT-E009-BAL-SHEET) — no pagination, fixed shape
    // -----------------------------------------------------------------

    public record BalanceSheetLine(
        String accountId,
        String accountCode,
        String accountName,
        long amount,
        String currency
    ) {
    }

    public record BalanceSheetResponse(
        String companyId,
        String currency,
        LocalDate asOfDate,
        List<BalanceSheetLine> assetLines,
        long totalAssets,
        List<BalanceSheetLine> liabilityLines,
        long totalLiabilities,
        List<BalanceSheetLine> equityLines,
        long totalEquity,
        boolean isBalanced
    ) {
    }
}

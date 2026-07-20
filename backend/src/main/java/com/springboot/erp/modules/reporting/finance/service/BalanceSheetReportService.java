package com.springboot.erp.modules.reporting.finance.service;

import com.springboot.erp.modules.finance.coa.domain.AccountType;
import com.springboot.erp.modules.finance.coa.domain.QAccount;
import com.springboot.erp.modules.finance.gl.domain.JournalEntryStatus;
import com.springboot.erp.modules.finance.gl.domain.QJournalEntry;
import com.springboot.erp.modules.finance.gl.domain.QJournalLine;
import com.springboot.erp.modules.reporting.finance.dto.FinanceReportDtos.BalanceSheetLine;
import com.springboot.erp.modules.reporting.finance.dto.FinanceReportDtos.BalanceSheetResponse;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RPT-E009-BAL-SHEET — Assets / Liabilities / Equity snapshot as of a cutoff date, no pagination.
 * Mirrors {@code balance_sheet()} in {@code app.reports.repositories.finance}: ASSET balance =
 * {@code SUM(debit) - SUM(credit)}; LIABILITY/EQUITY balance = {@code SUM(credit) - SUM(debit)}.
 *
 * <p>Since this system never posts year-end closing journals, the cumulative net income/expense
 * (INCOME/EXPENSE accounts' YTD activity up to {@code asOfDate}) is folded into equity as a
 * synthetic "Retained Earnings (YTD net income)" line (account_id null) — without it the
 * accounting identity {@code Assets = Liabilities + Equity + (Income - Expense)} cannot balance,
 * exactly as documented in the reference.
 */
@Service
@Transactional(readOnly = true)
public class BalanceSheetReportService {

    private final JPAQueryFactory query;

    public BalanceSheetReportService(JPAQueryFactory query) {
        this.query = query;
    }

    public BalanceSheetResponse run(String companyId, LocalDate asOfDate, String locationId) {
        String currency = "USD"; // display currency fallback — no Currency/Company lookup in this slice

        List<Tuple> assetRows = side(companyId, AccountType.ASSET, false, asOfDate, locationId);
        List<Tuple> liabilityRows = side(companyId, AccountType.LIABILITY, true, asOfDate, locationId);
        List<Tuple> equityRows = side(companyId, AccountType.EQUITY, true, asOfDate, locationId);

        long incomeTotal = netTotal(companyId, AccountType.INCOME, true, asOfDate, locationId);
        long expenseTotal = netTotal(companyId, AccountType.EXPENSE, false, asOfDate, locationId);
        long retainedEarnings = incomeTotal - expenseTotal;

        List<BalanceSheetLine> assetLines = shape(assetRows, currency);
        List<BalanceSheetLine> liabilityLines = shape(liabilityRows, currency);
        List<BalanceSheetLine> equityLines = new ArrayList<>(shape(equityRows, currency));
        if (retainedEarnings != 0) {
            equityLines.add(new BalanceSheetLine(null, "3999", "Retained Earnings (YTD net income)", retainedEarnings, currency));
        }

        long totalAssets = sumAmounts(assetRows);
        long totalLiabilities = sumAmounts(liabilityRows);
        long totalEquity = sumAmounts(equityRows) + retainedEarnings;

        return new BalanceSheetResponse(
            companyId, currency, asOfDate,
            assetLines, totalAssets,
            liabilityLines, totalLiabilities,
            equityLines, totalEquity,
            totalAssets == totalLiabilities + totalEquity);
    }

    private List<Tuple> side(String companyId, AccountType type, boolean creditMinusDebit,
                              LocalDate asOfDate, String locationId) {
        QAccount a = QAccount.account;
        QJournalLine jl = QJournalLine.journalLine;
        QJournalEntry je = QJournalEntry.journalEntry;

        BooleanExpression jeMatched = je.status.eq(JournalEntryStatus.POSTED)
            .and(je.companyId.eq(companyId))
            .and(je.entryDate.loe(asOfDate))
            .and(locationId != null ? je.locationId.eq(locationId) : null);

        NumberExpression<Long> sumD = new CaseBuilder().when(je.id.isNotNull()).then(jl.baseDebit).otherwise(0L).sum();
        NumberExpression<Long> sumC = new CaseBuilder().when(je.id.isNotNull()).then(jl.baseCredit).otherwise(0L).sum();
        NumberExpression<Long> amount = creditMinusDebit ? sumC.subtract(sumD) : sumD.subtract(sumC);

        return query.select(a.publicId, a.code, a.name, amount)
            .from(a)
            .leftJoin(jl).on(jl.accountId.eq(a.publicId))
            .leftJoin(jl.entry, je).on(jeMatched)
            .where(a.companyId.eq(companyId).and(a.type.eq(type)))
            .groupBy(a.id, a.publicId, a.code, a.name)
            .having(amount.ne(0L))
            .orderBy(a.code.asc())
            .fetch();
    }

    private long netTotal(String companyId, AccountType type, boolean creditMinusDebit,
                           LocalDate asOfDate, String locationId) {
        QAccount a = QAccount.account;
        QJournalLine jl = QJournalLine.journalLine;
        QJournalEntry je = QJournalEntry.journalEntry;

        BooleanExpression filter = a.companyId.eq(companyId)
            .and(a.type.eq(type))
            .and(je.companyId.eq(companyId))
            .and(je.status.eq(JournalEntryStatus.POSTED))
            .and(je.entryDate.loe(asOfDate))
            .and(locationId != null ? je.locationId.eq(locationId) : null);

        NumberExpression<Long> net = creditMinusDebit
            ? jl.baseCredit.sum().subtract(jl.baseDebit.sum())
            : jl.baseDebit.sum().subtract(jl.baseCredit.sum());

        Long result = query.select(net)
            .from(jl)
            .join(jl.entry, je)
            .join(a).on(a.publicId.eq(jl.accountId))
            .where(filter)
            .fetchOne();
        return result == null ? 0L : result;
    }

    private static List<BalanceSheetLine> shape(List<Tuple> rows, String currency) {
        List<BalanceSheetLine> out = new ArrayList<>(rows.size());
        for (Tuple r : rows) {
            long amt = r.get(3, Long.class) == null ? 0L : r.get(3, Long.class);
            out.add(new BalanceSheetLine(r.get(0, String.class), r.get(1, String.class), r.get(2, String.class), amt, currency));
        }
        return out;
    }

    private static long sumAmounts(List<Tuple> rows) {
        long total = 0L;
        for (Tuple r : rows) {
            Long amt = r.get(3, Long.class);
            total += amt == null ? 0L : amt;
        }
        return total;
    }
}

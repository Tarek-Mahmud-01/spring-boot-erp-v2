package com.springboot.erp.modules.reporting.finance.service;

import com.springboot.erp.modules.finance.coa.domain.Account;
import com.springboot.erp.modules.finance.gl.domain.HolderType;
import com.springboot.erp.modules.finance.gl.domain.JournalEntryStatus;
import com.springboot.erp.modules.finance.gl.domain.QJournalEntry;
import com.springboot.erp.modules.finance.gl.domain.QJournalLine;
import com.springboot.erp.modules.reporting.finance.dto.FinanceReportDtos.AccountLedgerRow;
import com.springboot.erp.modules.reporting.finance.dto.FinanceReportDtos.AccountLedgerSummary;
import com.springboot.erp.platform.web.PageResponse;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RPT-E009-ACC-LEDGER — chronological journal lines for one GL account (or, for a HEADER account,
 * every POSTING descendant in its nested-set window) with a running balance.
 *
 * <p>Reference: {@code account_ledger()} in {@code app.reports.repositories.finance}. The reference
 * computes the running balance with a windowed {@code SUM(...) OVER (...)}; Hibernate/QueryDSL has
 * no portable window-function DSL here, so the running balance is folded in Java over the
 * already-ordered, already-paginated page — O(page size), not O(table) — while the opening/period
 * totals still come from single QueryDSL aggregate queries exactly like the reference's Q2/Q3.
 */
@Service
@Transactional(readOnly = true)
public class AccountLedgerReportService {

    private final JPAQueryFactory query;
    private final FinanceReportSupport support;

    public AccountLedgerReportService(JPAQueryFactory query, FinanceReportSupport support) {
        this.query = query;
        this.support = support;
    }

    /** Paginated rows with a running balance seeded from the opening balance. */
    public PageResponse<AccountLedgerRow> list(String companyId, String accountId, LocalDate fromDate,
                                               LocalDate toDate, String voucherType, String locationId,
                                               Pageable pageable) {
        support.validateDateRange(fromDate, toDate);
        Account account = support.requireAccount(companyId, accountId);
        String currency = displayCurrency(account);

        QJournalLine jl = QJournalLine.journalLine;
        QJournalEntry je = QJournalEntry.journalEntry;
        BooleanExpression periodFilter = periodFilter(account, companyId, fromDate, toDate, voucherType, locationId);

        long opening = openingBalance(account, companyId, fromDate);
        long total = firstNonNull(query.select(jl.count()).from(jl).join(jl.entry, je).where(periodFilter).fetchOne());

        List<Tuple> rows = query.select(
                je.publicId, je.entryDate, je.voucherType, je.voucherNumber,
                jl.publicId, jl.narration, je.narration, je.reference,
                jl.baseDebit, jl.baseCredit, jl.holderType, jl.holderId)
            .from(jl).join(jl.entry, je)
            .where(periodFilter)
            .orderBy(je.entryDate.asc(), jl.id.asc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        List<AccountLedgerRow> out = new ArrayList<>(rows.size());
        long running = opening;
        for (Tuple r : rows) {
            long debit = firstNonNull(r.get(8, Long.class));
            long credit = firstNonNull(r.get(9, Long.class));
            running += debit - credit;
            out.add(new AccountLedgerRow(
                r.get(0, String.class), r.get(1, LocalDate.class), r.get(2, String.class), r.get(3, String.class),
                r.get(4, String.class), r.get(5, String.class), r.get(6, String.class), r.get(7, String.class),
                debit, credit, running, currency,
                r.get(10, HolderType.class).name(), r.get(11, String.class)));
        }
        return page(out, pageable, total);
    }

    /** Opening/total-debit/total-credit/closing aggregate block for the same filter set. */
    public AccountLedgerSummary summary(String companyId, String accountId, LocalDate fromDate, LocalDate toDate,
                                        String voucherType, String locationId) {
        support.validateDateRange(fromDate, toDate);
        Account account = support.requireAccount(companyId, accountId);
        String currency = displayCurrency(account);

        QJournalLine jl = QJournalLine.journalLine;
        QJournalEntry je = QJournalEntry.journalEntry;
        BooleanExpression periodFilter = periodFilter(account, companyId, fromDate, toDate, voucherType, locationId);

        long opening = openingBalance(account, companyId, fromDate);
        Tuple totals = query.select(jl.baseDebit.sum(), jl.baseCredit.sum())
            .from(jl).join(jl.entry, je)
            .where(periodFilter)
            .fetchOne();
        long periodDebit = totals == null ? 0L : firstNonNull(totals.get(0, Long.class));
        long periodCredit = totals == null ? 0L : firstNonNull(totals.get(1, Long.class));
        long closing = opening + periodDebit - periodCredit;
        long lineCount = firstNonNull(query.select(jl.count()).from(jl).join(jl.entry, je).where(periodFilter).fetchOne());

        return new AccountLedgerSummary(
            account.getPublicId(), account.getCode(), account.getName(), account.getType().name(),
            currency, opening, periodDebit, periodCredit, closing, lineCount);
    }

    private long openingBalance(Account account, String companyId, LocalDate fromDate) {
        if (fromDate == null) {
            return 0L;
        }
        QJournalLine jl = QJournalLine.journalLine;
        QJournalEntry je = QJournalEntry.journalEntry;
        BooleanExpression baseFilter = support.accountScopePredicate(account)
            .and(je.companyId.eq(companyId))
            .and(je.status.eq(JournalEntryStatus.POSTED))
            .and(je.entryDate.lt(fromDate));
        return firstNonNull(query.select(jl.baseDebit.sum().subtract(jl.baseCredit.sum()))
            .from(jl).join(jl.entry, je)
            .where(baseFilter)
            .fetchOne());
    }

    private BooleanExpression periodFilter(Account account, String companyId, LocalDate fromDate, LocalDate toDate,
                                           String voucherType, String locationId) {
        QJournalEntry je = QJournalEntry.journalEntry;
        return support.accountScopePredicate(account)
            .and(je.companyId.eq(companyId))
            .and(je.status.eq(JournalEntryStatus.POSTED))
            .and(fromDate != null ? je.entryDate.goe(fromDate) : null)
            .and(toDate != null ? je.entryDate.loe(toDate) : null)
            .and(voucherType != null ? je.voucherType.eq(voucherType.toUpperCase(Locale.ROOT)) : null)
            .and(locationId != null ? je.locationId.eq(locationId) : null);
    }

    private static String displayCurrency(Account account) {
        return account.getCurrency() != null ? account.getCurrency() : "USD";
    }

    private static <T> PageResponse<T> page(List<T> content, Pageable pageable, long total) {
        int totalPages = pageable.getPageSize() == 0 ? 0 : (int) Math.ceil((double) total / pageable.getPageSize());
        return new PageResponse<>(content, pageable.getPageNumber(), pageable.getPageSize(), total, totalPages);
    }

    private static long firstNonNull(Long value) {
        return value == null ? 0L : value;
    }
}

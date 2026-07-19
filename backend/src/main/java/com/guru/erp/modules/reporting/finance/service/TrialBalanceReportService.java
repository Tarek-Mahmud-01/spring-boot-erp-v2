package com.guru.erp.modules.reporting.finance.service;

import com.guru.erp.modules.finance.coa.domain.AccountType;
import com.guru.erp.modules.finance.coa.domain.QAccount;
import com.guru.erp.modules.finance.gl.domain.JournalEntryStatus;
import com.guru.erp.modules.finance.gl.domain.QJournalEntry;
import com.guru.erp.modules.finance.gl.domain.QJournalLine;
import com.guru.erp.modules.reporting.finance.dto.FinanceReportDtos.TrialBalanceRow;
import com.guru.erp.modules.reporting.finance.dto.FinanceReportDtos.TrialBalanceSummary;
import com.guru.erp.platform.web.PageResponse;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RPT-E009-TRIAL-BAL — one row per posting account with SUM(debit)/SUM(credit) totals for the
 * period; accounts with no postings still appear (LEFT JOIN), matching the reference's
 * {@code trial_balance()} in {@code app.reports.repositories.finance}.
 *
 * <p>{@code balance = total_debit - total_credit}: positive means the account carries a net debit
 * balance (its natural side for ASSET/EXPENSE), negative a net credit balance (natural for
 * LIABILITY/EQUITY/INCOME) — the reference does not flip sign by account type here; that
 * normal-balance interpretation is left to the caller/UI, exactly like the Python source.
 */
@Service
@Transactional(readOnly = true)
public class TrialBalanceReportService {

    private final JPAQueryFactory query;
    private final FinanceReportSupport support;

    public TrialBalanceReportService(JPAQueryFactory query, FinanceReportSupport support) {
        this.query = query;
        this.support = support;
    }

    /** Paginated per-account debit/credit/balance rows, ordered by account code. */
    public PageResponse<TrialBalanceRow> list(String companyId, LocalDate fromDate, LocalDate toDate,
                                              String locationId, Pageable pageable) {
        support.validateDateRange(fromDate, toDate);

        QAccount a = QAccount.account;
        QJournalLine jl = QJournalLine.journalLine;
        QJournalEntry je = QJournalEntry.journalEntry;

        long total = firstNonNull(query.select(a.count()).from(a).where(a.companyId.eq(companyId)).fetchOne());

        List<Tuple> rows = query.select(a.publicId, a.code, a.name, a.type, a.currency, totalDebit(je, jl), totalCredit(je, jl))
            .from(a)
            .leftJoin(jl).on(jl.accountId.eq(a.publicId))
            .leftJoin(jl.entry, je).on(jeMatched(companyId, fromDate, toDate, locationId))
            .where(a.companyId.eq(companyId))
            .groupBy(a.id, a.publicId, a.code, a.name, a.type, a.currency)
            .orderBy(a.code.asc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        List<TrialBalanceRow> out = new ArrayList<>(rows.size());
        for (Tuple r : rows) {
            String ccy = r.get(4, String.class);
            long td = firstNonNull(r.get(5, Long.class));
            long tc = firstNonNull(r.get(6, Long.class));
            out.add(new TrialBalanceRow(
                r.get(0, String.class), r.get(1, String.class), r.get(2, String.class),
                r.get(3, AccountType.class).name(), ccy, td, tc, td - tc));
        }
        return page(out, pageable, total);
    }

    /** Company-wide total debit/credit + balanced flag for the same filter set. */
    public TrialBalanceSummary summary(String companyId, LocalDate fromDate, LocalDate toDate, String locationId) {
        support.validateDateRange(fromDate, toDate);

        QJournalLine jl = QJournalLine.journalLine;
        QJournalEntry je = QJournalEntry.journalEntry;
        BooleanExpression companyWideFilter = je.status.eq(JournalEntryStatus.POSTED)
            .and(je.companyId.eq(companyId))
            .and(fromDate != null ? je.entryDate.goe(fromDate) : null)
            .and(toDate != null ? je.entryDate.loe(toDate) : null)
            .and(locationId != null ? je.locationId.eq(locationId) : null);

        Tuple totals = query.select(jl.baseDebit.sum(), jl.baseCredit.sum())
            .from(jl).join(jl.entry, je)
            .where(companyWideFilter)
            .fetchOne();
        long totalDebitAll = totals == null ? 0L : firstNonNull(totals.get(0, Long.class));
        long totalCreditAll = totals == null ? 0L : firstNonNull(totals.get(1, Long.class));

        // Company display currency: falls back to the first account's currency, else USD — this
        // slice has no separate Company/Currency lookup to port (loose cross-module reference).
        QAccount a = QAccount.account;
        String currency = query.select(a.currency).from(a)
            .where(a.companyId.eq(companyId).and(a.currency.isNotNull()))
            .orderBy(a.code.asc())
            .limit(1)
            .fetchFirst();
        String companyCurrency = currency != null ? currency : "USD";

        return new TrialBalanceSummary(companyId, companyCurrency, totalDebitAll, totalCreditAll, totalDebitAll == totalCreditAll);
    }

    private static BooleanExpression jeMatched(String companyId, LocalDate fromDate, LocalDate toDate, String locationId) {
        QJournalEntry je = QJournalEntry.journalEntry;
        return je.status.eq(JournalEntryStatus.POSTED)
            .and(je.companyId.eq(companyId))
            .and(fromDate != null ? je.entryDate.goe(fromDate) : null)
            .and(toDate != null ? je.entryDate.loe(toDate) : null)
            .and(locationId != null ? je.locationId.eq(locationId) : null);
    }

    // Guard mirrors the reference's CASE-wrapped SUM: a JournalLine can survive the LEFT JOIN with a
    // NULL entry when its parent entry fails the POSTED/date/location predicate (the join's ON
    // clause), so only lines whose entry actually matched contribute to the sums — otherwise a
    // TB-vs-sum-of-items divergence would appear, since the LEFT JOIN exists solely to keep
    // zero-postings accounts in the result.
    private static NumberExpression<Long> totalDebit(QJournalEntry je, QJournalLine jl) {
        return new CaseBuilder().when(je.id.isNotNull()).then(jl.baseDebit).otherwise(0L).sum();
    }

    private static NumberExpression<Long> totalCredit(QJournalEntry je, QJournalLine jl) {
        return new CaseBuilder().when(je.id.isNotNull()).then(jl.baseCredit).otherwise(0L).sum();
    }

    private static <T> PageResponse<T> page(List<T> content, Pageable pageable, long total) {
        int totalPages = pageable.getPageSize() == 0 ? 0 : (int) Math.ceil((double) total / pageable.getPageSize());
        return new PageResponse<>(content, pageable.getPageNumber(), pageable.getPageSize(), total, totalPages);
    }

    private static long firstNonNull(Long value) {
        return value == null ? 0L : value;
    }
}

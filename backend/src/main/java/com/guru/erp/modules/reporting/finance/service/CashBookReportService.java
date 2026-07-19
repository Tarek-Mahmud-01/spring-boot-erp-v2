package com.guru.erp.modules.reporting.finance.service;

import com.guru.erp.modules.finance.coa.domain.AccountPostingType;
import com.guru.erp.modules.finance.coa.domain.AccountType;
import com.guru.erp.modules.finance.coa.domain.QAccount;
import com.guru.erp.modules.finance.gl.domain.HolderType;
import com.guru.erp.modules.finance.gl.domain.JournalEntryStatus;
import com.guru.erp.modules.finance.gl.domain.QJournalEntry;
import com.guru.erp.modules.finance.gl.domain.QJournalLine;
import com.guru.erp.modules.reporting.finance.dto.FinanceReportDtos.CashBookRow;
import com.guru.erp.modules.reporting.finance.dto.FinanceReportDtos.CashBookSummary;
import com.guru.erp.platform.web.PageResponse;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RPT-E009-CASH-BOOK — chronological journal lines across every cash/bank GL account (ASSET
 * accounts whose code starts with "11", the standard Cash &amp; Bank block), with a running
 * balance. Mirrors {@code cash_book()} in {@code app.reports.repositories.finance}.
 *
 * <p>{@code payment_method_id} is accepted for API parity with the reference but is a documented
 * no-op there too (legacy SupplierPayment metadata that no longer exists once the report reads the
 * GL directly) — kept only so the endpoint signature matches; never applied as a filter.
 */
@Service
@Transactional(readOnly = true)
public class CashBookReportService {

    private static final String CURRENCY = "USD"; // display currency fallback — no Currency/Company lookup here

    private final JPAQueryFactory query;

    public CashBookReportService(JPAQueryFactory query) {
        this.query = query;
    }

    /** Paginated cash/bank journal lines with a running balance, oldest-first. */
    public PageResponse<CashBookRow> list(String companyId, LocalDate fromDate, LocalDate toDate, String voucherType,
                                          String locationId, Pageable pageable) {
        validateRange(fromDate, toDate);
        List<String> cashAccountIds = cashAccountIds(companyId);
        if (cashAccountIds.isEmpty()) {
            return page(List.of(), pageable, 0);
        }

        QJournalLine jl = QJournalLine.journalLine;
        QJournalEntry je = QJournalEntry.journalEntry;
        QAccount acct = new QAccount("cashBookAccount");

        BooleanExpression periodFilter = periodFilter(companyId, fromDate, toDate, voucherType, locationId, cashAccountIds);
        long opening = openingBalance(companyId, fromDate, voucherType, locationId, cashAccountIds);
        long total = firstNonNull(query.select(jl.count()).from(jl).join(jl.entry, je).where(periodFilter).fetchOne());

        List<Tuple> rows = query.select(
                je.publicId, je.entryDate, je.voucherType, je.voucherNumber, je.reference, je.narration,
                jl.publicId, jl.narration, jl.baseDebit, jl.baseCredit, jl.holderType, jl.holderId,
                acct.code, acct.name)
            .from(jl).join(jl.entry, je).join(acct).on(acct.publicId.eq(jl.accountId))
            .where(periodFilter)
            .orderBy(je.entryDate.asc(), je.id.asc(), jl.id.asc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        List<CashBookRow> out = new ArrayList<>(rows.size());
        long running = opening;
        for (Tuple r : rows) {
            long debit = firstNonNull(r.get(8, Long.class));
            long credit = firstNonNull(r.get(9, Long.class));
            running += debit - credit;
            out.add(new CashBookRow(
                r.get(0, String.class), r.get(1, LocalDate.class), r.get(2, String.class), r.get(3, String.class),
                r.get(6, String.class), r.get(7, String.class), r.get(5, String.class), r.get(4, String.class),
                r.get(12, String.class), r.get(13, String.class),
                debit, credit, running, CURRENCY,
                r.get(10, HolderType.class).name(), r.get(11, String.class)));
        }
        return page(out, pageable, total);
    }

    /** Opening/period-debit/period-credit/closing + account_count for the same filter set. */
    public CashBookSummary summary(String companyId, LocalDate fromDate, LocalDate toDate, String voucherType,
                                   String locationId) {
        validateRange(fromDate, toDate);
        List<String> cashAccountIds = cashAccountIds(companyId);
        if (cashAccountIds.isEmpty()) {
            return new CashBookSummary(companyId, CURRENCY, 0, 0, 0, 0, 0, 0);
        }

        QJournalLine jl = QJournalLine.journalLine;
        QJournalEntry je = QJournalEntry.journalEntry;
        BooleanExpression periodFilter = periodFilter(companyId, fromDate, toDate, voucherType, locationId, cashAccountIds);

        long opening = openingBalance(companyId, fromDate, voucherType, locationId, cashAccountIds);
        Tuple totals = query.select(jl.baseDebit.sum(), jl.baseCredit.sum())
            .from(jl).join(jl.entry, je)
            .where(periodFilter)
            .fetchOne();
        long periodDebit = totals == null ? 0L : firstNonNull(totals.get(0, Long.class));
        long periodCredit = totals == null ? 0L : firstNonNull(totals.get(1, Long.class));
        long closing = opening + periodDebit - periodCredit;
        long lineCount = firstNonNull(query.select(jl.count()).from(jl).join(jl.entry, je).where(periodFilter).fetchOne());

        return new CashBookSummary(companyId, CURRENCY, opening, periodDebit, periodCredit, closing, lineCount, cashAccountIds.size());
    }

    private List<String> cashAccountIds(String companyId) {
        QAccount a = QAccount.account;
        return query.select(a.publicId)
            .from(a)
            .where(a.companyId.eq(companyId)
                .and(a.type.eq(AccountType.ASSET))
                .and(a.code.startsWith("11"))
                .and(a.postingType.eq(AccountPostingType.POSTING)))
            .fetch();
    }

    private long openingBalance(String companyId, LocalDate fromDate, String voucherType, String locationId,
                                List<String> cashAccountIds) {
        if (fromDate == null) {
            return 0L;
        }
        QJournalLine jl = QJournalLine.journalLine;
        QJournalEntry je = QJournalEntry.journalEntry;
        BooleanExpression baseFilter = baseFilter(companyId, voucherType, locationId, cashAccountIds)
            .and(je.entryDate.lt(fromDate));
        return firstNonNull(query.select(jl.baseDebit.sum().subtract(jl.baseCredit.sum()))
            .from(jl).join(jl.entry, je)
            .where(baseFilter)
            .fetchOne());
    }

    private static BooleanExpression baseFilter(String companyId, String voucherType, String locationId,
                                                List<String> cashAccountIds) {
        QJournalLine jl = QJournalLine.journalLine;
        QJournalEntry je = QJournalEntry.journalEntry;
        return jl.accountId.in(cashAccountIds)
            .and(je.companyId.eq(companyId))
            .and(je.status.eq(JournalEntryStatus.POSTED))
            .and(locationId != null ? je.locationId.eq(locationId) : null)
            .and(voucherType != null ? je.voucherType.eq(voucherType) : null);
    }

    private static BooleanExpression periodFilter(String companyId, LocalDate fromDate, LocalDate toDate,
                                                  String voucherType, String locationId, List<String> cashAccountIds) {
        QJournalEntry je = QJournalEntry.journalEntry;
        return baseFilter(companyId, voucherType, locationId, cashAccountIds)
            .and(fromDate != null ? je.entryDate.goe(fromDate) : null)
            .and(toDate != null ? je.entryDate.loe(toDate) : null);
    }

    private static void validateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && toDate.isBefore(fromDate)) {
            throw new com.guru.erp.platform.error.DomainException(
                com.guru.erp.platform.error.ErrorCode.VALIDATION_FAILED, "toDate must be on or after fromDate");
        }
    }

    private static <T> PageResponse<T> page(List<T> content, Pageable pageable, long total) {
        int totalPages = pageable.getPageSize() == 0 ? 0 : (int) Math.ceil((double) total / pageable.getPageSize());
        return new PageResponse<>(content, pageable.getPageNumber(), pageable.getPageSize(), total, totalPages);
    }

    private static long firstNonNull(Long value) {
        return value == null ? 0L : value;
    }
}

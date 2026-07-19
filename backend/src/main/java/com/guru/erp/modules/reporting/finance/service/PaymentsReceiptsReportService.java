package com.guru.erp.modules.reporting.finance.service;

import com.guru.erp.modules.finance.coa.domain.AccountPurpose;
import com.guru.erp.modules.finance.coa.domain.QAccountMapping;
import com.guru.erp.modules.finance.gl.domain.HolderType;
import com.guru.erp.modules.finance.gl.domain.JournalEntryStatus;
import com.guru.erp.modules.finance.gl.domain.QJournalEntry;
import com.guru.erp.modules.finance.gl.domain.QJournalLine;
import com.guru.erp.modules.reporting.finance.dto.FinanceReportDtos.PaymentsReceiptsRow;
import com.guru.erp.modules.reporting.finance.dto.FinanceReportDtos.PaymentsReceiptsSummary;
import com.guru.erp.platform.web.PageResponse;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RPT-E009-PAY-RECEIPT — an all-methods cash book keyed off accounts that HOLD money
 * (CASH_ON_HAND / BANK_ACCOUNT mappings, any module), not off a fixed voucher-type list — mirrors
 * {@code payments_receipts()} in {@code app.reports.repositories.finance}. Direction = IN when the
 * cash leg is a debit, OUT when a credit; the amount reported is that leg's own base-currency value
 * (the real cash moved), not the whole entry.
 */
@Service
@Transactional(readOnly = true)
public class PaymentsReceiptsReportService {

    private static final String CURRENCY = "USD"; // display currency fallback — no Currency/Company lookup here

    private final JPAQueryFactory query;

    public PaymentsReceiptsReportService(JPAQueryFactory query) {
        this.query = query;
    }

    /** Paginated cash-leg rows, newest-first. */
    public PageResponse<PaymentsReceiptsRow> list(String companyId, LocalDate fromDate, LocalDate toDate,
                                                  String direction, String locationId, Pageable pageable) {
        validateRange(fromDate, toDate);
        List<String> cashAccountIds = cashAccountIds(companyId);
        if (cashAccountIds.isEmpty()) {
            return page(List.of(), pageable, 0);
        }

        QJournalLine jl = QJournalLine.journalLine;
        QJournalEntry je = QJournalEntry.journalEntry;
        QJournalLine other = new QJournalLine("otherLine");

        BooleanExpression isIn = jl.baseDebit.gt(0L);
        NumberExpression<Long> cashAmount = new CaseBuilder().when(isIn).then(jl.baseDebit).otherwise(jl.baseCredit);
        StringExpression directionExpr = new CaseBuilder().when(isIn).then("IN").otherwise("OUT");
        BooleanExpression filter = filter(companyId, fromDate, toDate, direction, locationId, cashAccountIds);

        long total = firstNonNull(query.select(jl.count()).from(jl).join(jl.entry, je).where(filter).fetchOne());

        StringExpression counterpartyType = Expressions.stringTemplate("({0})",
            JPAExpressions.select(other.holderType.stringValue().max())
                .from(other)
                .where(other.entry.eq(jl.entry)
                    .and(other.holderType.ne(HolderType.NONE))
                    .and(other.accountId.notIn(cashAccountIds))));
        StringExpression counterpartyId = Expressions.stringTemplate("({0})",
            JPAExpressions.select(other.holderId.max())
                .from(other)
                .where(other.entry.eq(jl.entry)
                    .and(other.holderId.isNotNull())
                    .and(other.accountId.notIn(cashAccountIds))));

        List<Tuple> rows = query.select(je.publicId, je.entryDate, je.voucherType, je.voucherNumber,
                directionExpr, counterpartyType, counterpartyId, je.reference, je.narration, cashAmount)
            .from(jl).join(jl.entry, je)
            .where(filter)
            .orderBy(je.entryDate.desc(), je.id.desc(), jl.id.asc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        List<PaymentsReceiptsRow> out = new ArrayList<>(rows.size());
        for (Tuple r : rows) {
            String narration = r.get(8, String.class);
            out.add(new PaymentsReceiptsRow(
                r.get(0, String.class), r.get(1, LocalDate.class), r.get(2, String.class), r.get(3, String.class),
                r.get(4, String.class), r.get(5, String.class), r.get(6, String.class), r.get(7, String.class),
                narration == null ? "" : narration, firstNonNull(r.get(9, Long.class)), CURRENCY));
        }
        return page(out, pageable, total);
    }

    /** Total in / total out / net + line count for the same filter set. */
    public PaymentsReceiptsSummary summary(String companyId, LocalDate fromDate, LocalDate toDate,
                                           String direction, String locationId) {
        validateRange(fromDate, toDate);
        List<String> cashAccountIds = cashAccountIds(companyId);
        if (cashAccountIds.isEmpty()) {
            return new PaymentsReceiptsSummary(companyId, CURRENCY, 0, 0, 0, 0);
        }

        QJournalLine jl = QJournalLine.journalLine;
        QJournalEntry je = QJournalEntry.journalEntry;
        BooleanExpression filter = filter(companyId, fromDate, toDate, direction, locationId, cashAccountIds);

        long total = firstNonNull(query.select(jl.count()).from(jl).join(jl.entry, je).where(filter).fetchOne());
        Tuple totals = query.select(jl.baseDebit.sum(), jl.baseCredit.sum())
            .from(jl).join(jl.entry, je)
            .where(filter)
            .fetchOne();
        long totalIn = totals == null ? 0L : firstNonNull(totals.get(0, Long.class));
        long totalOut = totals == null ? 0L : firstNonNull(totals.get(1, Long.class));

        return new PaymentsReceiptsSummary(companyId, CURRENCY, totalIn, totalOut, totalIn - totalOut, total);
    }

    private List<String> cashAccountIds(String companyId) {
        QAccountMapping am = QAccountMapping.accountMapping;
        return query.select(am.account.publicId).distinct()
            .from(am)
            .where(am.companyId.eq(companyId)
                .and(am.purpose.in(AccountPurpose.CASH_ON_HAND, AccountPurpose.BANK_ACCOUNT)))
            .fetch();
    }

    private static BooleanExpression filter(String companyId, LocalDate fromDate, LocalDate toDate, String direction,
                                            String locationId, List<String> cashAccountIds) {
        QJournalLine jl = QJournalLine.journalLine;
        QJournalEntry je = QJournalEntry.journalEntry;
        return je.companyId.eq(companyId)
            .and(je.status.eq(JournalEntryStatus.POSTED))
            .and(jl.accountId.in(cashAccountIds))
            .and(jl.baseDebit.gt(0L).or(jl.baseCredit.gt(0L)))
            .and("IN".equals(direction) ? jl.baseDebit.gt(0L) : null)
            .and("OUT".equals(direction) ? jl.baseCredit.gt(0L) : null)
            .and(fromDate != null ? je.entryDate.goe(fromDate) : null)
            .and(toDate != null ? je.entryDate.loe(toDate) : null)
            .and(locationId != null ? je.locationId.eq(locationId) : null);
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

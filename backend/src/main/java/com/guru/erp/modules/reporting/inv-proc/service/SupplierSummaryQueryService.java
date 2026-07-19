package com.guru.erp.modules.reporting.invproc.service;

import com.guru.erp.modules.finance.gl.domain.HolderType;
import com.guru.erp.modules.finance.gl.domain.JournalEntryStatus;
import com.guru.erp.modules.finance.gl.domain.QJournalEntry;
import com.guru.erp.modules.finance.gl.domain.QJournalLine;
import com.guru.erp.modules.procurement.suppliers.domain.QSupplier;
import com.guru.erp.modules.procurement.suppliers.domain.Supplier;
import com.guru.erp.modules.reporting.invproc.dto.InvProcReportDtos.SupplierSummaryRow;
import com.guru.erp.modules.reporting.invproc.dto.InvProcReportDtos.SupplierSummarySummary;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Supplier Summary (RPT-E004-SUP-SUMMARY) — aggregated AP payable per supplier (reference
 * {@code app.reports.repositories.procurement.supplier_summary}). One GROUP BY: LEFT JOIN
 * {@link Supplier} -&gt; {@code JournalLine} (rows tagged {@code holderType=SUPPLIER},
 * {@code holderId=supplier.publicId}) -&gt; {@code JournalEntry} (POSTED only). Suppliers with no
 * posted activity in the window still surface with zero balances unless {@code onlyOpen} filters
 * them out. Balance convention: {@code credit - debit} (+ opening balance, FX-converted) — positive
 * means the company owes the supplier.
 */
@Service
@Transactional(readOnly = true)
public class SupplierSummaryQueryService {

    private final JPAQueryFactory queryFactory;
    private final ReportSupport support;

    public SupplierSummaryQueryService(JPAQueryFactory queryFactory, ReportSupport support) {
        this.queryFactory = queryFactory;
        this.support = support;
    }

    public record Result(List<SupplierSummaryRow> rows, SupplierSummarySummary summary, long total) {
    }

    public Result run(LocalDate fromDate, LocalDate toDate, boolean onlyOpen, String currency, Pageable pageable) {
        support.validateRange(fromDate, toDate);
        QSupplier s = QSupplier.supplier;
        QJournalLine jl = QJournalLine.journalLine;
        QJournalEntry je = QJournalEntry.journalEntry;

        BooleanExpression jlJoin = jl.holderType.eq(HolderType.SUPPLIER).and(jl.holderId.eq(s.publicId));
        BooleanExpression jeJoin = je.id.eq(jl.entry.id).and(je.status.eq(JournalEntryStatus.POSTED));
        if (fromDate != null) {
            jeJoin = jeJoin.and(je.entryDate.goe(fromDate));
        }
        if (toDate != null) {
            jeJoin = jeJoin.and(je.entryDate.loe(toDate));
        }

        var debitSum = jl.baseDebit.sum();
        var creditSum = jl.baseCredit.sum();
        List<Tuple> grouped = queryFactory
            .select(s.publicId, s.code, s.name,
                s.openingBalance.amountMinor, s.openingBalanceSide, s.openingBalanceDate, s.openingBalanceExchangeRate,
                debitSum, creditSum)
            .from(s)
            .leftJoin(jl).on(jlJoin)
            .leftJoin(je).on(jeJoin)
            .groupBy(s.id, s.publicId, s.code, s.name,
                s.openingBalance.amountMinor, s.openingBalanceSide, s.openingBalanceDate, s.openingBalanceExchangeRate)
            .fetch();

        List<Tuple> filtered = new ArrayList<>();
        for (Tuple t : grouped) {
            if (!onlyOpen || fullBalance(t, s, debitSum, creditSum, fromDate) != 0L) {
                filtered.add(t);
            }
        }
        filtered.sort((a, b) -> a.get(s.code).compareTo(b.get(s.code)));

        long total = filtered.size();
        int from = Math.min(pageable.getPageNumber() * pageable.getPageSize(), filtered.size());
        int to = Math.min(from + pageable.getPageSize(), filtered.size());

        long totalDebit = 0L;
        long totalCredit = 0L;
        long netPayable = 0L;
        for (Tuple t : filtered) {
            totalDebit += nz(t.get(debitSum));
            totalCredit += nz(t.get(creditSum));
            netPayable += fullBalance(t, s, debitSum, creditSum, fromDate);
        }

        List<SupplierSummaryRow> rows = new ArrayList<>();
        for (Tuple t : filtered.subList(from, to)) {
            rows.add(new SupplierSummaryRow(
                t.get(s.publicId), t.get(s.code), t.get(s.name),
                nz(t.get(debitSum)), nz(t.get(creditSum)), fullBalance(t, s, debitSum, creditSum, fromDate), currency));
        }

        SupplierSummarySummary summary = new SupplierSummarySummary(currency, filtered.size(), totalDebit, totalCredit, netPayable);
        return new Result(rows, summary, total);
    }

    /** Journal net (credit - debit) plus the supplier's FX-converted, date-gated opening balance. */
    private long fullBalance(Tuple t, QSupplier s,
                              com.querydsl.core.types.dsl.NumberExpression<Long> debitSum,
                              com.querydsl.core.types.dsl.NumberExpression<Long> creditSum,
                              LocalDate fromDate) {
        long journalNet = nz(t.get(creditSum)) - nz(t.get(debitSum));
        Long obAmount = t.get(s.openingBalance.amountMinor);
        String obSide = t.get(s.openingBalanceSide);
        LocalDate obDate = t.get(s.openingBalanceDate);
        BigDecimal obRate = t.get(s.openingBalanceExchangeRate);
        if (fromDate != null && obDate != null && !obDate.isBefore(fromDate)) {
            return journalNet;
        }
        long amount = obAmount == null ? 0L : obAmount;
        long signed = "CREDIT".equals(obSide) ? amount : -amount;
        BigDecimal rate = obRate == null ? BigDecimal.ONE : obRate;
        long ob = BigDecimal.valueOf(signed).multiply(rate).setScale(0, RoundingMode.HALF_EVEN).longValueExact();
        return journalNet + ob;
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }
}

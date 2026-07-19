package com.guru.erp.modules.reporting.finance.service;

import com.guru.erp.modules.finance.coa.domain.AccountType;
import com.guru.erp.modules.finance.coa.domain.QAccount;
import com.guru.erp.modules.finance.gl.domain.JournalEntryStatus;
import com.guru.erp.modules.finance.gl.domain.QJournalEntry;
import com.guru.erp.modules.finance.gl.domain.QJournalLine;
import com.guru.erp.modules.reporting.finance.dto.FinanceReportDtos.IncomeStatementLine;
import com.guru.erp.modules.reporting.finance.dto.FinanceReportDtos.IncomeStatementResponse;
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
 * RPT-E009-INCOME-STMT — revenue / expense aggregation for a fixed period, no pagination.
 * Mirrors {@code income_statement()} in {@code app.reports.repositories.finance}: INCOME accounts
 * net as {@code SUM(credit) - SUM(debit)} (revenue increases on credit), EXPENSE accounts net as
 * {@code SUM(debit) - SUM(credit)}. Only accounts with a non-zero net for the period are returned
 * (the reference's {@code HAVING amount != 0}).
 */
@Service
@Transactional(readOnly = true)
public class IncomeStatementReportService {

    private final JPAQueryFactory query;

    public IncomeStatementReportService(JPAQueryFactory query) {
        this.query = query;
    }

    public IncomeStatementResponse run(String companyId, LocalDate fromDate, LocalDate toDate, String locationId) {
        if (toDate.isBefore(fromDate)) {
            throw new com.guru.erp.platform.error.DomainException(
                com.guru.erp.platform.error.ErrorCode.VALIDATION_FAILED, "toDate must be on or after fromDate");
        }
        String currency = "USD"; // display currency fallback — no Currency/Company lookup in this slice

        Side income = side(companyId, AccountType.INCOME, true, fromDate, toDate, locationId, currency);
        Side expense = side(companyId, AccountType.EXPENSE, false, fromDate, toDate, locationId, currency);

        return new IncomeStatementResponse(
            companyId, currency, fromDate, toDate,
            income.lines, income.total,
            expense.lines, expense.total,
            income.total - expense.total);
    }

    private record Side(List<IncomeStatementLine> lines, long total) {
    }

    private Side side(String companyId, AccountType type, boolean creditMinusDebit,
                       LocalDate fromDate, LocalDate toDate, String locationId, String currency) {
        QAccount a = QAccount.account;
        QJournalLine jl = QJournalLine.journalLine;
        QJournalEntry je = QJournalEntry.journalEntry;

        BooleanExpression jeMatched = je.status.eq(JournalEntryStatus.POSTED)
            .and(je.companyId.eq(companyId))
            .and(je.entryDate.goe(fromDate))
            .and(je.entryDate.loe(toDate))
            .and(locationId != null ? je.locationId.eq(locationId) : null);

        NumberExpression<Long> sumD = new CaseBuilder().when(je.id.isNotNull()).then(jl.baseDebit).otherwise(0L).sum();
        NumberExpression<Long> sumC = new CaseBuilder().when(je.id.isNotNull()).then(jl.baseCredit).otherwise(0L).sum();
        NumberExpression<Long> amount = creditMinusDebit ? sumC.subtract(sumD) : sumD.subtract(sumC);

        List<Tuple> rows = query.select(a.publicId, a.code, a.name, amount)
            .from(a)
            .leftJoin(jl).on(jl.accountId.eq(a.publicId))
            .leftJoin(jl.entry, je).on(jeMatched)
            .where(a.companyId.eq(companyId).and(a.type.eq(type)))
            .groupBy(a.id, a.publicId, a.code, a.name)
            .having(amount.ne(0L))
            .orderBy(a.code.asc())
            .fetch();

        List<IncomeStatementLine> lines = new ArrayList<>(rows.size());
        long total = 0L;
        for (Tuple r : rows) {
            long amt = r.get(3, Long.class) == null ? 0L : r.get(3, Long.class);
            total += amt;
            lines.add(new IncomeStatementLine(r.get(0, String.class), r.get(1, String.class), r.get(2, String.class), amt, currency));
        }
        return new Side(lines, total);
    }
}

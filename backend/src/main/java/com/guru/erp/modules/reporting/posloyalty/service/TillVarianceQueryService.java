package com.guru.erp.modules.reporting.posloyalty.service;

import com.guru.erp.modules.access.domain.QUser;
import com.guru.erp.modules.pos.registers.domain.QPosTillSession;
import com.guru.erp.modules.reporting.posloyalty.dto.PosOpsReportDtos.TillVarianceResponse;
import com.guru.erp.modules.reporting.posloyalty.dto.PosOpsReportDtos.TillVarianceRow;
import com.guru.erp.modules.reporting.posloyalty.dto.PosOpsReportDtos.TillVarianceSummary;
import com.guru.erp.platform.web.PageResponse;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RPT-019 — Till Variance (reference {@code repositories/pos_ops.py::till_variance}). Lists
 * {@code PosTillSession}s with their cash-count variance, optionally filtered to over-threshold
 * variances, plus a summary (session count + total signed variance) computed over the FULL
 * filtered set — not just the current page, mirroring the reference's separate rollup query.
 */
@Service
@Transactional(readOnly = true)
public class TillVarianceQueryService {

    private final JPAQueryFactory queryFactory;
    private final PosLoyaltyReportSupport support;

    public TillVarianceQueryService(JPAQueryFactory queryFactory, PosLoyaltyReportSupport support) {
        this.queryFactory = queryFactory;
        this.support = support;
    }

    public TillVarianceResponse tillVariance(String companyId, String locationId, String cashierId,
                                             long overThreshold, LocalDate fromDate, LocalDate toDate,
                                             Pageable pageable) {
        support.validateDateRange(fromDate, toDate);
        String baseCurrency = support.baseCurrency(companyId);
        List<String> companyLocationIds = support.locationIdsForCompany(companyId);
        if (companyLocationIds.isEmpty()) {
            return new TillVarianceResponse(PageResponse.of(Page.empty(pageable)),
                new TillVarianceSummary(companyId, 0, 0, baseCurrency));
        }

        QPosTillSession session = QPosTillSession.posTillSession;
        QUser cashier = new QUser("tillCashier");
        QUser approver = new QUser("tillApprover");

        var closedOrOpened = session.closedAt.coalesce(session.openedAt);
        BooleanExpression predicate = session.locationId.in(companyLocationIds);
        if (locationId != null) {
            predicate = predicate.and(session.locationId.eq(locationId));
        }
        if (cashierId != null) {
            predicate = predicate.and(cashier.publicId.eq(cashierId));
        }
        if (overThreshold > 0) {
            predicate = predicate.and(Expressions.numberTemplate(Long.class, "abs(coalesce({0}, 0))",
                session.varianceAmount).goe(overThreshold));
        }
        if (fromDate != null) {
            predicate = predicate.and(closedOrOpened.goe(support.startOfDayUtc(fromDate)));
        }
        if (toDate != null) {
            predicate = predicate.and(closedOrOpened.loe(support.endOfDayUtc(toDate)));
        }

        var baseQuery = queryFactory
            .select(session.publicId, session.status, session.openingFloat.amountMinor,
                session.expectedCash.amountMinor, session.countedCash.amountMinor, session.varianceAmount,
                session.openingFloat.currency, session.openedAt, session.closedAt, session.locationId,
                cashier.publicId, cashier.fullName, approver.fullName, session.varianceApprovedAt)
            .from(session)
            .leftJoin(cashier).on(cashier.publicId.eq(session.cashierId))
            .leftJoin(approver).on(approver.publicId.eq(session.varianceApprovedBy))
            .where(predicate);

        long total = baseQuery.fetchCount();
        List<Tuple> tuples = baseQuery
            .orderBy(session.openedAt.desc(), session.id.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        List<TillVarianceRow> rows = tuples.stream().map(t -> {
            Long expected = t.get(session.expectedCash.amountMinor);
            Long variance = t.get(session.varianceAmount);
            Double pct = (expected != null && expected != 0 && variance != null)
                ? Math.round(variance.doubleValue() / expected * 100 * 100.0) / 100.0
                : null;
            return new TillVarianceRow(
                t.get(session.publicId),
                t.get(session.status).name(),
                orZero(t.get(session.openingFloat.amountMinor)),
                expected,
                t.get(session.countedCash.amountMinor),
                variance,
                t.get(session.openingFloat.currency),
                t.get(session.openedAt),
                t.get(session.closedAt),
                t.get(session.locationId),
                t.get(cashier.publicId),
                orDash(t.get(cashier.fullName)),
                pct,
                t.get(approver.fullName),
                t.get(session.varianceApprovedAt));
        }).toList();

        Page<TillVarianceRow> page = new PageImpl<>(rows, pageable, total);

        Long rollup = queryFactory
            .select(session.varianceAmount.sum())
            .from(session)
            .leftJoin(cashier).on(cashier.publicId.eq(session.cashierId))
            .where(predicate)
            .fetchOne();
        String currency = rows.isEmpty() ? baseCurrency : rows.get(0).currency();
        TillVarianceSummary summary = new TillVarianceSummary(companyId, total, orZero(rollup), currency);
        return new TillVarianceResponse(PageResponse.of(page), summary);
    }

    private static long orZero(Long v) {
        return v == null ? 0L : v;
    }

    private static String orDash(String v) {
        return v == null || v.isBlank() ? "—" : v;
    }
}

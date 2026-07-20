package com.springboot.erp.modules.reporting.posloyalty.service;

import com.springboot.erp.modules.access.domain.QUser;
import com.springboot.erp.modules.pos.auxiliary.domain.QPosParkedSale;
import com.springboot.erp.modules.pos.transactions.domain.QPosTransaction;
import com.springboot.erp.modules.pos.transactions.domain.QPosTransactionLine;
import com.springboot.erp.modules.reporting.posloyalty.dto.PosOpsReportDtos.AbandonedCartResponse;
import com.springboot.erp.modules.reporting.posloyalty.dto.PosOpsReportDtos.AbandonedCartRow;
import com.springboot.erp.modules.reporting.posloyalty.dto.PosOpsReportDtos.AbandonedCartSummary;
import com.springboot.erp.platform.web.PageResponse;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RPT-033 — Suspended / Abandoned Carts (reference {@code repositories/pos_ops.py::abandoned_carts}).
 * Lists not-yet-resumed {@code PosParkedSale} rows, deriving ACTIVE/EXPIRED from {@code expiresAt}
 * vs now (computed once in Java, matching the reference), joined to the parked transaction's total
 * and line count.
 */
@Service
@Transactional(readOnly = true)
public class AbandonedCartQueryService {

    private final JPAQueryFactory queryFactory;
    private final PosLoyaltyReportSupport support;

    public AbandonedCartQueryService(JPAQueryFactory queryFactory, PosLoyaltyReportSupport support) {
        this.queryFactory = queryFactory;
        this.support = support;
    }

    public AbandonedCartResponse abandonedCarts(String companyId, String statusFilter, LocalDate fromDate,
                                                LocalDate toDate, Pageable pageable) {
        support.validateDateRange(fromDate, toDate);
        String baseCurrency = support.baseCurrency(companyId);
        List<String> companyLocationIds = support.locationIdsForCompany(companyId);
        if (companyLocationIds.isEmpty()) {
            return new AbandonedCartResponse(PageResponse.of(Page.empty(pageable)),
                new AbandonedCartSummary(companyId, 0, 0, 0, 0, baseCurrency));
        }

        QPosParkedSale park = QPosParkedSale.posParkedSale;
        QPosTransaction txn = QPosTransaction.posTransaction;
        QUser parkedByUser = QUser.user;

        BooleanExpression predicate = park.resumedAt.isNull().and(park.locationId.in(companyLocationIds));
        if (fromDate != null) {
            predicate = predicate.and(park.parkedAt.goe(support.startOfDayUtc(fromDate)));
        }
        if (toDate != null) {
            predicate = predicate.and(park.parkedAt.loe(support.endOfDayUtc(toDate)));
        }

        List<Tuple> tuples = queryFactory
            .select(park.publicId, park.parkCode, park.parkedAt, park.expiresAt, park.transactionId,
                txn.totalAmount, txn.currency, parkedByUser.fullName)
            .from(park)
            .join(txn).on(txn.publicId.eq(park.transactionId))
            .leftJoin(parkedByUser).on(parkedByUser.publicId.eq(park.parkedBy))
            .where(predicate)
            .orderBy(park.parkedAt.desc(), park.id.desc())
            .fetch();

        List<String> txnIds = tuples.stream().map(t -> t.get(park.transactionId)).toList();
        Map<String, Long> lineCounts = new java.util.HashMap<>();
        if (!txnIds.isEmpty()) {
            QPosTransactionLine line = QPosTransactionLine.posTransactionLine;
            var txnIdPath = line.transaction.publicId;
            var countPath = line.count();
            for (Tuple t : queryFactory
                .select(txnIdPath, countPath)
                .from(line)
                .where(txnIdPath.in(txnIds))
                .groupBy(txnIdPath)
                .fetch()) {
                lineCounts.put(t.get(txnIdPath), t.get(countPath));
            }
        }

        Instant now = Instant.now();
        String statusUpper = statusFilter == null ? null : statusFilter.toUpperCase(Locale.ROOT);
        List<AbandonedCartRow> out = new ArrayList<>();
        for (Tuple t : tuples) {
            Instant expires = t.get(park.expiresAt);
            Instant parkedAt = t.get(park.parkedAt);
            boolean expired = expires != null && expires.isBefore(now);
            String status = expired ? "EXPIRED" : "ACTIVE";
            if (statusUpper != null && !status.equals(statusUpper)) {
                continue;
            }
            long ageHours = parkedAt == null ? 0 : Math.max(0,
                Math.round(java.time.Duration.between(parkedAt, now).toMillis() / 3_600_000.0));
            out.add(new AbandonedCartRow(
                t.get(park.publicId),
                t.get(park.parkCode),
                parkedAt,
                orDash(t.get(parkedByUser.fullName)),
                expires,
                lineCounts.getOrDefault(t.get(park.transactionId), 0L),
                orZero(t.get(txn.totalAmount)),
                t.get(txn.currency) == null ? baseCurrency : t.get(txn.currency),
                status,
                ageHours));
        }

        long total = out.size();
        int from = Math.min((int) pageable.getOffset(), out.size());
        int to = Math.min(from + pageable.getPageSize(), out.size());
        Page<AbandonedCartRow> page = new PageImpl<>(out.subList(from, to), pageable, total);

        long active = out.stream().filter(r -> "ACTIVE".equals(r.status())).count();
        long expired = out.stream().filter(r -> "EXPIRED".equals(r.status())).count();
        long value = out.stream().mapToLong(AbandonedCartRow::totalMinor).sum();
        String currency = out.isEmpty() ? baseCurrency : out.get(0).currency();
        AbandonedCartSummary summary = new AbandonedCartSummary(companyId, total, active, expired, value, currency);
        return new AbandonedCartResponse(PageResponse.of(page), summary);
    }

    private static long orZero(Long v) {
        return v == null ? 0L : v;
    }

    private static String orDash(String v) {
        return v == null || v.isBlank() ? "—" : v;
    }
}

package com.guru.erp.modules.reporting.posloyalty.service;

import com.guru.erp.modules.pos.transactions.domain.PosTransactionStatus;
import com.guru.erp.modules.pos.transactions.domain.PosTransactionType;
import com.guru.erp.modules.pos.transactions.domain.QPosTender;
import com.guru.erp.modules.pos.transactions.domain.QPosTransaction;
import com.guru.erp.modules.reporting.posloyalty.dto.PosOpsReportDtos.DailySalesResponse;
import com.guru.erp.modules.reporting.posloyalty.dto.PosOpsReportDtos.DailySalesRow;
import com.guru.erp.modules.reporting.posloyalty.dto.PosOpsReportDtos.DailySalesSummary;
import com.guru.erp.platform.web.PageResponse;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RPT-018 — POS Daily Sales Summary (reference {@code repositories/pos_ops.py::daily_sales}).
 * Buckets completed sale/refund transactions by (day, location) then folds in the tender split
 * (cash/card/store-credit/other), matching the reference's two-pass Python aggregation exactly —
 * a refund's payout tender is stored as a positive magnitude, same as a sale's; only the parent
 * transaction's type says whether it reduces or adds to the method total.
 */
@Service
@Transactional(readOnly = true)
public class DailySalesQueryService {

    private final JPAQueryFactory queryFactory;
    private final PosLoyaltyReportSupport support;

    public DailySalesQueryService(JPAQueryFactory queryFactory, PosLoyaltyReportSupport support) {
        this.queryFactory = queryFactory;
        this.support = support;
    }

    private record Bucket(String date, String locationId) {
    }

    private static final class Agg {
        long receiptCount;
        long refundCount;
        long gross;
        long refund;
        long tax;
        long cash;
        long card;
        long storeCredit;
        long other;
        String currency;
    }

    public DailySalesResponse dailySales(String companyId, String locationId, LocalDate fromDate, LocalDate toDate,
                                         Pageable pageable) {
        support.validateDateRange(fromDate, toDate);
        String baseCurrency = support.baseCurrency(companyId);
        List<String> companyLocationIds = support.locationIdsForCompany(companyId);
        if (companyLocationIds.isEmpty()) {
            return new DailySalesResponse(PageResponse.of(Page.empty(pageable)),
                new DailySalesSummary(companyId, 0, 0, 0, 0, 0, baseCurrency));
        }

        QPosTransaction txn = QPosTransaction.posTransaction;
        BooleanExpression predicate = txn.status.eq(PosTransactionStatus.COMPLETED)
            .and(txn.locationId.in(companyLocationIds));
        if (locationId != null) {
            predicate = predicate.and(txn.locationId.eq(locationId));
        }
        if (fromDate != null) {
            predicate = predicate.and(txn.occurredAt.goe(support.startOfDayUtc(fromDate)));
        }
        if (toDate != null) {
            predicate = predicate.and(txn.occurredAt.loe(support.endOfDayUtc(toDate)));
        }

        List<Tuple> txns = queryFactory
            .select(txn.id, txn.type, txn.occurredAt, txn.locationId, txn.subtotalAmount, txn.taxAmount, txn.currency)
            .from(txn)
            .where(predicate)
            .fetch();

        Map<Bucket, Agg> buckets = new LinkedHashMap<>();
        Map<Long, Bucket> txnMeta = new LinkedHashMap<>();
        Map<Long, PosTransactionType> txnType = new LinkedHashMap<>();
        for (Tuple t : txns) {
            Long id = t.get(txn.id);
            String day = t.get(txn.occurredAt).atZone(ZoneOffset.UTC).toLocalDate().toString();
            Bucket key = new Bucket(day, t.get(txn.locationId));
            txnMeta.put(id, key);
            txnType.put(id, t.get(txn.type));
            Agg agg = buckets.computeIfAbsent(key, k -> {
                Agg a = new Agg();
                a.currency = t.get(txn.currency);
                return a;
            });
            if (t.get(txn.type) == PosTransactionType.SALE) {
                agg.receiptCount++;
                agg.gross += orZero(t.get(txn.subtotalAmount));
                agg.tax += orZero(t.get(txn.taxAmount));
            } else if (t.get(txn.type) == PosTransactionType.REFUND) {
                agg.refundCount++;
                agg.refund += Math.abs(orZero(t.get(txn.subtotalAmount)));
            }
        }

        if (!txnMeta.isEmpty()) {
            QPosTender tender = QPosTender.posTender;
            List<Tuple> tenders = queryFactory
                .select(tender.transaction.id, tender.methodType, tender.amountAmount)
                .from(tender)
                .where(tender.transaction.id.in(txnMeta.keySet()))
                .fetch();
            for (Tuple t : tenders) {
                Long tid = t.get(tender.transaction.id);
                Bucket key = txnMeta.get(tid);
                if (key == null) {
                    continue;
                }
                Agg agg = buckets.get(key);
                String method = t.get(tender.methodType) == null ? "" : t.get(tender.methodType).toUpperCase(Locale.ROOT);
                long amt = orZero(t.get(tender.amountAmount));
                if (txnType.get(tid) == PosTransactionType.REFUND) {
                    amt = -amt;
                }
                switch (method) {
                    case "CASH" -> agg.cash += amt;
                    case "CARD" -> agg.card += amt;
                    case "STORE_CREDIT" -> agg.storeCredit += amt;
                    default -> agg.other += amt;
                }
            }
        }

        List<DailySalesRow> rows = new ArrayList<>();
        for (Map.Entry<Bucket, Agg> e : buckets.entrySet()) {
            Agg a = e.getValue();
            long net = a.gross - a.refund;
            rows.add(new DailySalesRow(LocalDate.parse(e.getKey().date()), e.getKey().locationId(),
                a.receiptCount, a.refundCount, a.gross, a.refund, a.tax, a.cash, a.card, a.storeCredit,
                a.other, net, a.currency));
        }
        rows.sort((r1, r2) -> {
            int byDate = r2.date().compareTo(r1.date());
            return byDate != 0 ? byDate : r1.locationId().compareTo(r2.locationId());
        });

        long total = rows.size();
        int from = Math.min((int) pageable.getOffset(), rows.size());
        int to = Math.min(from + pageable.getPageSize(), rows.size());
        Page<DailySalesRow> page = new PageImpl<>(rows.subList(from, to), pageable, total);

        String currency = rows.isEmpty() ? baseCurrency : rows.get(0).currency();
        DailySalesSummary summary = new DailySalesSummary(companyId, total,
            rows.stream().mapToLong(DailySalesRow::grossMinor).sum(),
            rows.stream().mapToLong(DailySalesRow::refundMinor).sum(),
            rows.stream().mapToLong(DailySalesRow::taxMinor).sum(),
            rows.stream().mapToLong(DailySalesRow::netMinor).sum(),
            currency);
        return new DailySalesResponse(PageResponse.of(page), summary);
    }

    private static long orZero(Long v) {
        return v == null ? 0L : v;
    }
}

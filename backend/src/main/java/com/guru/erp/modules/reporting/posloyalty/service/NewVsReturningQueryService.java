package com.guru.erp.modules.reporting.posloyalty.service;

import com.guru.erp.modules.pos.transactions.domain.PosTransactionStatus;
import com.guru.erp.modules.pos.transactions.domain.PosTransactionType;
import com.guru.erp.modules.pos.transactions.domain.QPosTransaction;
import com.guru.erp.modules.reporting.posloyalty.dto.PosOpsReportDtos.NewVsReturningResponse;
import com.guru.erp.modules.reporting.posloyalty.dto.PosOpsReportDtos.NewVsReturningRow;
import com.guru.erp.modules.reporting.posloyalty.dto.PosOpsReportDtos.NewVsReturningSummary;
import com.guru.erp.platform.web.PageResponse;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RPT-032 — New vs Returning Customers (reference
 * {@code repositories/pos_ops.py::new_vs_returning}). A customer's FIRST completed-sale calendar
 * day (over the company's ENTIRE sale history, unbounded by the report's date filter — mirroring
 * the reference) decides whether each of their receipts inside the filtered window counts as
 * "new" or "returning"; walk-in (no attached customer) receipts are tracked separately.
 */
@Service
@Transactional(readOnly = true)
public class NewVsReturningQueryService {

    private static final Set<String> WALK_IN = Set.of("", "cust_walk_in");

    private final JPAQueryFactory queryFactory;
    private final PosLoyaltyReportSupport support;

    public NewVsReturningQueryService(JPAQueryFactory queryFactory, PosLoyaltyReportSupport support) {
        this.queryFactory = queryFactory;
        this.support = support;
    }

    private static final class Agg {
        long newCustomers;
        long returningCustomers;
        long anonymousReceipts;
        long newRevenue;
        long returningRevenue;
        long anonymousRevenue;
        String currency;
        final Set<String> newSeen = new HashSet<>();
        final Set<String> returningSeen = new HashSet<>();
    }

    public NewVsReturningResponse newVsReturning(String companyId, String locationId, LocalDate fromDate,
                                                 LocalDate toDate, Pageable pageable) {
        support.validateDateRange(fromDate, toDate);
        String baseCurrency = support.baseCurrency(companyId);
        List<String> companyLocationIds = support.locationIdsForCompany(companyId);
        if (companyLocationIds.isEmpty()) {
            return new NewVsReturningResponse(PageResponse.of(Page.empty(pageable)),
                new NewVsReturningSummary(companyId, 0, 0, 0, 0, 0, 0, 0, baseCurrency));
        }

        QPosTransaction txn = QPosTransaction.posTransaction;
        List<Tuple> sales = queryFactory
            .select(txn.customerId, txn.occurredAt, txn.locationId, txn.subtotalAmount, txn.currency)
            .from(txn)
            .where(txn.type.eq(PosTransactionType.SALE), txn.status.eq(PosTransactionStatus.COMPLETED),
                txn.locationId.in(companyLocationIds))
            .fetch();

        Map<String, String> firstDay = new LinkedHashMap<>();
        for (Tuple s : sales) {
            String cust = s.get(txn.customerId);
            if (cust == null || WALK_IN.contains(cust)) {
                continue;
            }
            String day = s.get(txn.occurredAt).atZone(ZoneOffset.UTC).toLocalDate().toString();
            String cur = firstDay.get(cust);
            if (cur == null || day.compareTo(cur) < 0) {
                firstDay.put(cust, day);
            }
        }

        Map<String, Agg> buckets = new LinkedHashMap<>();
        for (Tuple s : sales) {
            String rowLocation = s.get(txn.locationId);
            if (locationId != null && !locationId.equals(rowLocation)) {
                continue;
            }
            LocalDate d = s.get(txn.occurredAt).atZone(ZoneOffset.UTC).toLocalDate();
            if (fromDate != null && d.isBefore(fromDate)) {
                continue;
            }
            if (toDate != null && d.isAfter(toDate)) {
                continue;
            }
            String day = d.toString();
            Agg agg = buckets.computeIfAbsent(day, k -> {
                Agg a = new Agg();
                a.currency = s.get(txn.currency);
                return a;
            });
            long amount = s.get(txn.subtotalAmount) == null ? 0L : s.get(txn.subtotalAmount);
            String cust = s.get(txn.customerId);
            if (cust == null || WALK_IN.contains(cust)) {
                agg.anonymousReceipts++;
                agg.anonymousRevenue += amount;
                continue;
            }
            if (day.equals(firstDay.get(cust))) {
                agg.newRevenue += amount;
                if (agg.newSeen.add(cust)) {
                    agg.newCustomers++;
                }
            } else {
                agg.returningRevenue += amount;
                if (agg.returningSeen.add(cust)) {
                    agg.returningCustomers++;
                }
            }
        }

        List<NewVsReturningRow> rows = new ArrayList<>();
        for (Map.Entry<String, Agg> e : buckets.entrySet()) {
            Agg a = e.getValue();
            rows.add(new NewVsReturningRow(LocalDate.parse(e.getKey()), a.newCustomers, a.returningCustomers,
                a.anonymousReceipts, a.newRevenue, a.returningRevenue, a.anonymousRevenue, a.currency));
        }
        rows.sort((r1, r2) -> r2.date().compareTo(r1.date()));

        long total = rows.size();
        int from = Math.min((int) pageable.getOffset(), rows.size());
        int to = Math.min(from + pageable.getPageSize(), rows.size());
        Page<NewVsReturningRow> page = new PageImpl<>(rows.subList(from, to), pageable, total);

        String currency = rows.isEmpty() ? baseCurrency : rows.get(0).currency();
        NewVsReturningSummary summary = new NewVsReturningSummary(companyId, total,
            rows.stream().mapToLong(NewVsReturningRow::newCustomers).sum(),
            rows.stream().mapToLong(NewVsReturningRow::returningCustomers).sum(),
            rows.stream().mapToLong(NewVsReturningRow::anonymousReceipts).sum(),
            rows.stream().mapToLong(NewVsReturningRow::newRevenueMinor).sum(),
            rows.stream().mapToLong(NewVsReturningRow::returningRevenueMinor).sum(),
            rows.stream().mapToLong(NewVsReturningRow::anonymousRevenueMinor).sum(),
            currency);
        return new NewVsReturningResponse(PageResponse.of(page), summary);
    }
}

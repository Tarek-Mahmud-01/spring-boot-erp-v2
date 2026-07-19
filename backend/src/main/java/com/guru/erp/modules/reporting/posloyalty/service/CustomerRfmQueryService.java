package com.guru.erp.modules.reporting.posloyalty.service;

import com.guru.erp.modules.crm.customers.domain.QCustomer;
import com.guru.erp.modules.crm.loyalty.domain.QCustomerTransaction;
import com.guru.erp.modules.crm.loyalty.domain.QLoyaltyAccount;
import com.guru.erp.modules.crm.loyalty.domain.TransactionType;
import com.guru.erp.modules.reporting.posloyalty.dto.LoyaltyAnalyticsDtos.CustomerRfmResponse;
import com.guru.erp.modules.reporting.posloyalty.dto.LoyaltyAnalyticsDtos.CustomerRfmRow;
import com.guru.erp.modules.reporting.posloyalty.dto.LoyaltyAnalyticsDtos.CustomerRfmSummary;
import com.guru.erp.modules.reporting.posloyalty.service.LoyaltyProgramSupport.Program;
import com.guru.erp.platform.web.PageResponse;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
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
 * RPT-031 — Customer RFM / Lifetime-Value Cohorts (reference
 * {@code repositories/loyalty_analytics.py::customer_rfm}). Population quintile scoring over
 * Recency/Frequency/Monetary from the {@code CustomerTransaction} purchase-history projection, then
 * bucketed into the standard 10-segment RFM matrix. Quintile scoring needs the WHOLE population's
 * value distribution at once (bisection over a sorted array), so — like the reference — this runs
 * in Java rather than as a SQL window function.
 */
@Service
@Transactional(readOnly = true)
public class CustomerRfmQueryService {

    private static final Set<String> WALK_IN = Set.of("cust_walk_in");

    private final JPAQueryFactory queryFactory;
    private final LoyaltyProgramSupport programSupport;
    private final PosLoyaltyReportSupport support;

    public CustomerRfmQueryService(JPAQueryFactory queryFactory, LoyaltyProgramSupport programSupport,
                                   PosLoyaltyReportSupport support) {
        this.queryFactory = queryFactory;
        this.programSupport = programSupport;
        this.support = support;
    }

    private static final class Agg {
        long frequency;
        long monetary;
        String first;
        String last;
        String currency;
        String name;
        String membershipId;
        String tierId;
    }

    public CustomerRfmResponse customerRfm(String companyId, LocalDate fromDate, LocalDate toDate, String rfmSegment,
                                           String tierId, Pageable pageable) {
        support.validateDateRange(fromDate, toDate);
        Program program = programSupport.loadProgram(companyId);
        Map<String, String> tierName = programSupport.tierNameMap(program);

        QCustomerTransaction txn = QCustomerTransaction.customerTransaction;
        QCustomer customer = QCustomer.customer;
        QLoyaltyAccount account = QLoyaltyAccount.loyaltyAccount;

        BooleanExpression predicate = customer.companyId.eq(companyId);
        if (fromDate != null) {
            predicate = predicate.and(txn.occurredAt.goe(support.startOfDayUtc(fromDate)));
        }
        if (toDate != null) {
            predicate = predicate.and(txn.occurredAt.loe(support.endOfDayUtc(toDate)));
        }

        List<Tuple> rows = queryFactory
            .select(customer.publicId, customer.firstName, customer.lastName, customer.membershipId,
                account.tierId, txn.type, txn.totalAmount, txn.totalCurrency, txn.occurredAt)
            .from(txn)
            .join(customer).on(customer.publicId.eq(txn.customerId))
            .leftJoin(account).on(account.customerId.eq(customer.publicId))
            .where(predicate)
            .orderBy(customer.id.asc())
            .fetch();

        Map<String, Agg> acc = new LinkedHashMap<>();
        for (Tuple r : rows) {
            String cid = r.get(customer.publicId);
            if (WALK_IN.contains(cid)) {
                continue;
            }
            Agg a = acc.computeIfAbsent(cid, k -> {
                Agg agg = new Agg();
                agg.currency = r.get(txn.totalCurrency) == null ? program.currency() : r.get(txn.totalCurrency);
                agg.name = (r.get(customer.firstName) + " " + r.get(customer.lastName)).strip();
                agg.membershipId = r.get(customer.membershipId);
                agg.tierId = r.get(account.tierId);
                return agg;
            });
            String iso = r.get(txn.occurredAt).toString();
            long amount = r.get(txn.totalAmount) == null ? 0L : r.get(txn.totalAmount);
            if (r.get(txn.type) == TransactionType.SALE) {
                a.frequency++;
                a.monetary += amount;
                if (a.first == null || iso.compareTo(a.first) < 0) {
                    a.first = iso;
                }
                if (a.last == null || iso.compareTo(a.last) > 0) {
                    a.last = iso;
                }
            } else if (r.get(txn.type) == TransactionType.REFUND) {
                a.monetary -= Math.abs(amount);
            }
        }

        Map<String, Agg> scored = new LinkedHashMap<>();
        for (Map.Entry<String, Agg> e : acc.entrySet()) {
            if (e.getValue().frequency > 0) {
                scored.put(e.getKey(), e.getValue());
            }
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<Long> recencyVals = new ArrayList<>();
        List<Long> freqVals = new ArrayList<>();
        List<Long> monVals = new ArrayList<>();
        for (Agg a : scored.values()) {
            recencyVals.add(daysSince(a.last, today));
            freqVals.add(a.frequency);
            monVals.add(a.monetary);
        }
        Collections.sort(recencyVals);
        Collections.sort(freqVals);
        Collections.sort(monVals);

        List<CustomerRfmRow> result = new ArrayList<>();
        for (Map.Entry<String, Agg> e : scored.entrySet()) {
            Agg a = e.getValue();
            long rec = daysSince(a.last, today);
            int rscore = 6 - quintile(rec, recencyVals);
            int fscore = quintile(a.frequency, freqVals);
            int mscore = quintile(a.monetary, monVals);
            String segment = rfmSegment(rscore, fscore, mscore);
            result.add(new CustomerRfmRow(e.getKey(), a.name, a.membershipId, a.tierId,
                a.tierId == null ? "Not enrolled" : tierName.getOrDefault(a.tierId, "—"), rec, a.frequency,
                a.monetary, a.currency, rscore, fscore, mscore, segment,
                a.first == null ? null : Instant.parse(a.first), a.last == null ? null : Instant.parse(a.last)));
        }

        List<CustomerRfmRow> filtered = new ArrayList<>();
        for (CustomerRfmRow r : result) {
            if (rfmSegment != null && !rfmSegment.equals(r.rfmSegment())) {
                continue;
            }
            if (tierId != null && !tierId.equals(r.tierId())) {
                continue;
            }
            filtered.add(r);
        }
        filtered.sort((x, y) -> {
            String xl = x.lastPurchaseAt() == null ? "" : x.lastPurchaseAt().toString();
            String yl = y.lastPurchaseAt() == null ? "" : y.lastPurchaseAt().toString();
            return yl.compareTo(xl);
        });
        filtered.sort((x, y) -> Long.compare(y.monetaryMinor(), x.monetaryMinor()));

        long total = filtered.size();
        int from = Math.min((int) pageable.getOffset(), filtered.size());
        int to = Math.min(from + pageable.getPageSize(), filtered.size());
        Page<CustomerRfmRow> page = new PageImpl<>(filtered.subList(from, to), pageable, total);

        long champions = filtered.stream().filter(r -> "Champions".equals(r.rfmSegment())).count();
        double avgRecency = total > 0
            ? Math.round(filtered.stream().mapToLong(CustomerRfmRow::recencyDays).average().orElse(0) * 10.0) / 10.0
            : 0.0;
        CustomerRfmSummary summary = new CustomerRfmSummary(companyId, total,
            filtered.stream().mapToLong(CustomerRfmRow::monetaryMinor).sum(), champions, avgRecency, program.currency());
        return new CustomerRfmResponse(PageResponse.of(page), summary);
    }

    private static long daysSince(String isoInstant, LocalDate today) {
        LocalDate d = Instant.parse(isoInstant).atZone(ZoneOffset.UTC).toLocalDate();
        return java.time.temporal.ChronoUnit.DAYS.between(d, today);
    }

    private static int quintile(long value, List<Long> sortedAsc) {
        if (sortedAsc.isEmpty()) {
            return 1;
        }
        int lo = lowerBound(sortedAsc, value);
        double pct = lo / (double) sortedAsc.size();
        return Math.min(5, (int) (pct * 5) + 1);
    }

    private static int lowerBound(List<Long> sortedAsc, long value) {
        int idx = Collections.binarySearch(sortedAsc, value);
        if (idx >= 0) {
            while (idx > 0 && sortedAsc.get(idx - 1) == value) {
                idx--;
            }
            return idx;
        }
        return -idx - 1;
    }

    private static String rfmSegment(int r, int f, int m) {
        int fm = Math.round((f + m) / 2.0f);
        if (r >= 4 && fm >= 4) {
            return "Champions";
        }
        if (r >= 3 && fm >= 3) {
            return "Loyal customers";
        }
        if (r >= 4 && fm <= 2) {
            return "New customers";
        }
        if (r >= 3 && fm <= 2) {
            return "Potential loyalist";
        }
        if (r == 2 && fm >= 4) {
            return "Need attention";
        }
        if (r == 2 && fm == 3) {
            return "Promising";
        }
        if (r == 2 && fm <= 2) {
            return "About to sleep";
        }
        if (r == 1 && fm >= 4) {
            return "Can't lose them";
        }
        if (r == 1 && fm == 3) {
            return "At risk";
        }
        if (r == 1 && fm <= 2) {
            return "Hibernating";
        }
        return "Others";
    }
}

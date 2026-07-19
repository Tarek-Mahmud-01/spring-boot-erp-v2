package com.guru.erp.modules.reporting.posloyalty.service;

import com.guru.erp.modules.crm.customers.domain.CustomerStatus;
import com.guru.erp.modules.crm.customers.domain.QCustomer;
import com.guru.erp.modules.crm.loyalty.domain.QLoyaltyAccount;
import com.guru.erp.modules.reporting.posloyalty.dto.LoyaltyAnalyticsDtos.TierDistributionResponse;
import com.guru.erp.modules.reporting.posloyalty.dto.LoyaltyAnalyticsDtos.TierDistributionRow;
import com.guru.erp.modules.reporting.posloyalty.dto.LoyaltyAnalyticsDtos.TierDistributionSummary;
import com.guru.erp.modules.reporting.posloyalty.service.LoyaltyProgramSupport.Program;
import com.guru.erp.platform.web.PageResponse;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RPT-030 — Tier Distribution &amp; Migration (reference
 * {@code repositories/loyalty_analytics.py::tier_distribution}). Buckets every active, non-deleted
 * customer by their currently assigned loyalty tier (plus a synthetic "Not enrolled" bucket), and
 * flags members whose rolling-12-month spend now qualifies them for a different tier than the one
 * assigned (due-upgrade / due-downgrade), exactly mirroring the reference's tier-rank comparison.
 */
@Service
@Transactional(readOnly = true)
public class TierDistributionQueryService {

    private final JPAQueryFactory queryFactory;
    private final LoyaltyProgramSupport programSupport;

    public TierDistributionQueryService(JPAQueryFactory queryFactory, LoyaltyProgramSupport programSupport) {
        this.queryFactory = queryFactory;
        this.programSupport = programSupport;
    }

    private static final class Row {
        String tierId;
        String tierCode;
        String tierName;
        long minSpend;
        double earnMultiplier;
        long memberCount;
        long dueUpgrade;
        long dueDowngrade;
        long total12mSpend;
    }

    public TierDistributionResponse tierDistribution(String companyId, Pageable pageable) {
        Program program = programSupport.loadProgram(companyId);
        List<Map<String, Object>> tiers = new ArrayList<>(program.tiers());
        tiers.sort(java.util.Comparator.comparingLong(t -> asLong(t.get("minSpendAmount"))));
        Map<String, Integer> rank = new java.util.HashMap<>();
        for (int i = 0; i < tiers.size(); i++) {
            Object id = tiers.get(i).get("id");
            if (id != null) {
                rank.put(String.valueOf(id), i);
            }
        }

        QCustomer customer = QCustomer.customer;
        QLoyaltyAccount account = QLoyaltyAccount.loyaltyAccount;
        List<Tuple> customers = queryFactory
            .select(account.tierId, account.rolling12mSpendAmount)
            .from(customer)
            .leftJoin(account).on(account.customerId.eq(customer.publicId))
            .where(customer.companyId.eq(companyId), customer.status.eq(CustomerStatus.ACTIVE))
            .fetch();

        Map<String, Row> rows = new LinkedHashMap<>();
        for (Map<String, Object> t : tiers) {
            seed(rows, String.valueOf(t.get("id")), t, program.currency());
        }
        seed(rows, null, Map.of("code", "NONE", "name", "Not enrolled", "minSpendAmount", 0L, "earnMultiplier", 1.0),
            program.currency());

        for (Tuple c : customers) {
            String assigned = c.get(account.tierId);
            long spend = orZero(c.get(account.rolling12mSpendAmount));
            Map<String, Object> tierMeta = assigned == null ? null
                : tiers.stream().filter(t -> assigned.equals(String.valueOf(t.get("id")))).findFirst().orElse(null);
            Row row = (assigned != null && tierMeta != null)
                ? rows.get(assigned)
                : rows.get(null);
            row.memberCount++;
            row.total12mSpend += spend;
            if (assigned != null && tierMeta != null) {
                String qualifying = qualifyingTier(tiers, spend);
                int arank = rank.getOrDefault(assigned, 0);
                int qrank = qualifying == null ? -1 : rank.getOrDefault(qualifying, 0);
                if (qrank > arank) {
                    row.dueUpgrade++;
                } else if (qrank < arank) {
                    row.dueDowngrade++;
                }
            }
        }

        List<TierDistributionRow> result = new ArrayList<>();
        for (Row r : rows.values()) {
            long avg = r.memberCount > 0 ? Math.round(r.total12mSpend / (double) r.memberCount) : 0;
            result.add(new TierDistributionRow(r.tierId, r.tierCode, r.tierName, r.minSpend, r.earnMultiplier,
                program.currency(), r.memberCount, r.dueUpgrade, r.dueDowngrade, r.total12mSpend, avg));
        }
        result.sort((x, y) -> Integer.compare(
            y.tierId() == null ? -1 : rank.getOrDefault(y.tierId(), 0),
            x.tierId() == null ? -1 : rank.getOrDefault(x.tierId(), 0)));

        long total = result.size();
        int from = Math.min((int) pageable.getOffset(), result.size());
        int to = Math.min(from + pageable.getPageSize(), result.size());
        Page<TierDistributionRow> page = new PageImpl<>(result.subList(from, to), pageable, total);

        TierDistributionSummary summary = new TierDistributionSummary(companyId,
            result.stream().mapToLong(TierDistributionRow::memberCount).sum(),
            result.stream().mapToLong(TierDistributionRow::dueUpgrade).sum(),
            result.stream().mapToLong(TierDistributionRow::dueDowngrade).sum(),
            result.stream().mapToLong(TierDistributionRow::total12mSpendMinor).sum(), program.currency());
        return new TierDistributionResponse(PageResponse.of(page), summary);
    }

    private static void seed(Map<String, Row> rows, String tierId, Map<String, Object> t, String currency) {
        rows.computeIfAbsent(tierId, k -> {
            Row r = new Row();
            r.tierId = tierId;
            r.tierCode = String.valueOf(t.getOrDefault("code", ""));
            r.tierName = String.valueOf(t.getOrDefault("name", "—"));
            r.minSpend = asLong(t.get("minSpendAmount"));
            Object mult = t.get("earnMultiplier");
            r.earnMultiplier = mult == null ? 1.0 : ((Number) mult).doubleValue();
            return r;
        });
    }

    private static String qualifyingTier(List<Map<String, Object>> tiersAscending, long spend) {
        String match = null;
        for (Map<String, Object> t : tiersAscending) {
            if (spend >= asLong(t.get("minSpendAmount"))) {
                match = String.valueOf(t.get("id"));
            }
        }
        return match;
    }

    private static long asLong(Object v) {
        return v == null ? 0L : ((Number) v).longValue();
    }

    private static long orZero(Long v) {
        return v == null ? 0L : v;
    }
}

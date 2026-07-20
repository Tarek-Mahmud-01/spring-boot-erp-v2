package com.springboot.erp.modules.reporting.posloyalty.service;

import com.springboot.erp.modules.crm.customers.domain.QCustomer;
import com.springboot.erp.modules.crm.loyalty.domain.LoyaltyMovementType;
import com.springboot.erp.modules.crm.loyalty.domain.QLoyaltyAccount;
import com.springboot.erp.modules.crm.loyalty.domain.QLoyaltyLedger;
import com.springboot.erp.modules.reporting.posloyalty.dto.LoyaltyAnalyticsDtos.LoyaltyLiabilityResponse;
import com.springboot.erp.modules.reporting.posloyalty.dto.LoyaltyAnalyticsDtos.LoyaltyLiabilityRow;
import com.springboot.erp.modules.reporting.posloyalty.dto.LoyaltyAnalyticsDtos.LoyaltyLiabilitySummary;
import com.springboot.erp.modules.reporting.posloyalty.service.LoyaltyProgramSupport.Program;
import com.springboot.erp.platform.web.PageResponse;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
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
 * RPT-028 — Loyalty Liability &amp; Breakage (reference
 * {@code repositories/loyalty_analytics.py::loyalty_liability}). Replays every ledger movement per
 * customer into a running balance, earned/redeemed/expired/reversed buckets, and a points -> minor
 * currency liability valuation; breakage % = expired / earned across the whole company.
 */
@Service
@Transactional(readOnly = true)
public class LoyaltyLiabilityQueryService {

    private final JPAQueryFactory queryFactory;
    private final LoyaltyProgramSupport programSupport;

    public LoyaltyLiabilityQueryService(JPAQueryFactory queryFactory, LoyaltyProgramSupport programSupport) {
        this.queryFactory = queryFactory;
        this.programSupport = programSupport;
    }

    private static final class Agg {
        String customerId;
        String customerName;
        String membershipId;
        String tierId;
        long balance;
        long earned;
        long redeemed;
        long expired;
        long reversed;
        Instant lastActivityAt;
    }

    public LoyaltyLiabilityResponse loyaltyLiability(String companyId, String tierId, boolean nonZeroOnly,
                                                     Pageable pageable) {
        Program program = programSupport.loadProgram(companyId);
        Map<String, String> tierName = programSupport.tierNameMap(program);

        QLoyaltyLedger ledger = QLoyaltyLedger.loyaltyLedger;
        QCustomer customer = QCustomer.customer;
        QLoyaltyAccount account = QLoyaltyAccount.loyaltyAccount;

        List<Tuple> rows = queryFactory
            .select(customer.publicId, customer.firstName, customer.lastName, customer.membershipId,
                account.tierId, ledger.type, ledger.pointsSigned, ledger.occurredAt)
            .from(ledger)
            .join(customer).on(customer.publicId.eq(ledger.customerId))
            .leftJoin(account).on(account.customerId.eq(customer.publicId))
            .where(customer.companyId.eq(companyId))
            .orderBy(customer.id.asc())
            .fetch();

        Map<String, Agg> byCustomer = new LinkedHashMap<>();
        for (Tuple r : rows) {
            String cid = r.get(customer.publicId);
            Agg a = byCustomer.computeIfAbsent(cid, k -> {
                Agg agg = new Agg();
                agg.customerId = cid;
                agg.customerName = (r.get(customer.firstName) + " " + r.get(customer.lastName)).strip();
                agg.membershipId = r.get(customer.membershipId);
                agg.tierId = r.get(account.tierId);
                return agg;
            });
            long pts = r.get(ledger.pointsSigned);
            a.balance += pts;
            LoyaltyMovementType type = r.get(ledger.type);
            switch (type) {
                case EARN -> a.earned += pts;
                case REDEEM -> a.redeemed += Math.abs(pts);
                case EXPIRE -> a.expired += Math.abs(pts);
                case REVERSE -> a.reversed += Math.abs(pts);
            }
            Instant occurred = r.get(ledger.occurredAt);
            if (occurred != null && (a.lastActivityAt == null || occurred.isAfter(a.lastActivityAt))) {
                a.lastActivityAt = occurred;
            }
        }

        List<LoyaltyLiabilityRow> result = new ArrayList<>();
        for (Agg a : byCustomer.values()) {
            if (tierId != null && !tierId.equals(a.tierId)) {
                continue;
            }
            if (nonZeroOnly && a.balance <= 0) {
                continue;
            }
            long liability = programSupport.pointsToMinor(Math.max(0, a.balance), program.pointsPerCurrencyUnit());
            result.add(new LoyaltyLiabilityRow(a.customerId, a.customerName, a.membershipId, a.tierId,
                a.tierId == null ? "Not enrolled" : tierName.getOrDefault(a.tierId, "—"), a.balance, a.earned,
                a.redeemed, a.expired, a.reversed, liability, program.currency(), a.lastActivityAt));
        }
        result.sort((x, y) -> {
            int byLiability = Long.compare(y.liabilityMinor(), x.liabilityMinor());
            return byLiability != 0 ? byLiability : x.membershipId().compareTo(y.membershipId());
        });

        long total = result.size();
        int from = Math.min((int) pageable.getOffset(), result.size());
        int to = Math.min(from + pageable.getPageSize(), result.size());
        Page<LoyaltyLiabilityRow> page = new PageImpl<>(result.subList(from, to), pageable, total);

        long totalEarned = result.stream().mapToLong(LoyaltyLiabilityRow::pointsEarned).sum();
        long totalExpired = result.stream().mapToLong(LoyaltyLiabilityRow::pointsExpired).sum();
        double breakagePct = totalEarned > 0 ? Math.round((totalExpired / (double) totalEarned) * 100 * 100.0) / 100.0 : 0.0;
        LoyaltyLiabilitySummary summary = new LoyaltyLiabilitySummary(companyId, total,
            result.stream().mapToLong(LoyaltyLiabilityRow::pointsBalance).sum(),
            result.stream().mapToLong(LoyaltyLiabilityRow::liabilityMinor).sum(), totalEarned, totalExpired,
            breakagePct, program.currency());
        return new LoyaltyLiabilityResponse(PageResponse.of(page), summary);
    }
}

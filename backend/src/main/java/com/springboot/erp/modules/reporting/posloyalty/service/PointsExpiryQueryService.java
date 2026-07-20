package com.springboot.erp.modules.reporting.posloyalty.service;

import com.springboot.erp.modules.crm.customers.domain.QCustomer;
import com.springboot.erp.modules.crm.loyalty.domain.QLoyaltyAccount;
import com.springboot.erp.modules.crm.loyalty.domain.QLoyaltyLedger;
import com.springboot.erp.modules.reporting.posloyalty.dto.LoyaltyAnalyticsDtos.PointsExpiryResponse;
import com.springboot.erp.modules.reporting.posloyalty.dto.LoyaltyAnalyticsDtos.PointsExpiryRow;
import com.springboot.erp.modules.reporting.posloyalty.dto.LoyaltyAnalyticsDtos.PointsExpirySummary;
import com.springboot.erp.modules.reporting.posloyalty.service.LoyaltyProgramSupport.Program;
import com.springboot.erp.platform.web.PageResponse;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
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
 * RPT-029 — Points Expiry Forecast (reference {@code repositories/loyalty_analytics.py::points_expiry}),
 * FIFO earn-lot replay per customer: each EARN row opens a lot; each REDEEM/EXPIRE/REVERSE consumes
 * the oldest still-open lots first. Surviving lots are bucketed into 30/60/90-day expiry windows
 * (based on {@code earnedDate + expiryMonths}) plus an overdue bucket, exactly mirroring the
 * reference's Python FIFO loop (portable across DBs; not expressible as a single SQL aggregate).
 */
@Service
@Transactional(readOnly = true)
public class PointsExpiryQueryService {

    private final JPAQueryFactory queryFactory;
    private final LoyaltyProgramSupport programSupport;

    public PointsExpiryQueryService(JPAQueryFactory queryFactory, LoyaltyProgramSupport programSupport) {
        this.queryFactory = queryFactory;
        this.programSupport = programSupport;
    }

    private record Meta(String name, String membershipId, String tierId) {
    }

    private static final class Lot {
        LocalDate earnedDate;
        long remaining;

        Lot(LocalDate earnedDate, long remaining) {
            this.earnedDate = earnedDate;
            this.remaining = remaining;
        }
    }

    public PointsExpiryResponse pointsExpiry(String companyId, String tierId, String withinDays, Pageable pageable) {
        Program program = programSupport.loadProgram(companyId);
        Map<String, String> tierName = programSupport.tierNameMap(program);
        boolean never = program.expiryMonths() <= 0;
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        QLoyaltyLedger ledger = QLoyaltyLedger.loyaltyLedger;
        QCustomer customer = QCustomer.customer;
        QLoyaltyAccount account = QLoyaltyAccount.loyaltyAccount;

        List<Tuple> rows = queryFactory
            .select(customer.publicId, customer.firstName, customer.lastName, customer.membershipId,
                account.tierId, ledger.pointsSigned, ledger.occurredAt)
            .from(ledger)
            .join(customer).on(customer.publicId.eq(ledger.customerId))
            .leftJoin(account).on(account.customerId.eq(customer.publicId))
            .where(customer.companyId.eq(companyId))
            .orderBy(customer.id.asc(), ledger.occurredAt.asc(), ledger.id.asc())
            .fetch();

        Map<String, List<Tuple>> byCustomer = new LinkedHashMap<>();
        Map<String, Meta> meta = new LinkedHashMap<>();
        for (Tuple r : rows) {
            String cid = r.get(customer.publicId);
            byCustomer.computeIfAbsent(cid, k -> new ArrayList<>()).add(r);
            meta.putIfAbsent(cid, new Meta((r.get(customer.firstName) + " " + r.get(customer.lastName)).strip(),
                r.get(customer.membershipId), r.get(account.tierId)));
        }

        List<PointsExpiryRow> result = new ArrayList<>();
        for (Map.Entry<String, List<Tuple>> e : byCustomer.entrySet()) {
            List<Lot> lots = new ArrayList<>();
            for (Tuple r : e.getValue()) {
                long pts = r.get(ledger.pointsSigned);
                if (pts > 0) {
                    lots.add(new Lot(r.get(ledger.occurredAt).atZone(ZoneOffset.UTC).toLocalDate(), pts));
                } else {
                    long toConsume = -pts;
                    for (Lot lot : lots) {
                        if (toConsume <= 0) {
                            break;
                        }
                        long take = Math.min(lot.remaining, toConsume);
                        lot.remaining -= take;
                        toConsume -= take;
                    }
                }
            }
            List<Lot> live = lots.stream().filter(l -> l.remaining > 0).toList();
            long balance = live.stream().mapToLong(l -> l.remaining).sum();
            if (balance <= 0) {
                continue;
            }

            long e30 = 0;
            long e60 = 0;
            long e90 = 0;
            long overdue = 0;
            LocalDate nextAt = null;
            long nextPts = 0;
            if (!never) {
                for (Lot lot : live) {
                    LocalDate exp = addMonths(lot.earnedDate, program.expiryMonths());
                    long days = java.time.temporal.ChronoUnit.DAYS.between(today, exp);
                    if (days < 0) {
                        overdue += lot.remaining;
                    } else {
                        if (days <= 30) {
                            e30 += lot.remaining;
                        }
                        if (days <= 60) {
                            e60 += lot.remaining;
                        }
                        if (days <= 90) {
                            e90 += lot.remaining;
                        }
                        if (nextAt == null || exp.isBefore(nextAt)) {
                            nextAt = exp;
                            nextPts = lot.remaining;
                        } else if (exp.equals(nextAt)) {
                            nextPts += lot.remaining;
                        }
                    }
                }
            }

            Meta m = meta.get(e.getKey());
            result.add(new PointsExpiryRow(e.getKey(), m.name(), m.membershipId(), m.tierId(),
                m.tierId() == null ? "Not enrolled" : tierName.getOrDefault(m.tierId(), "—"), balance, e30, e60, e90,
                overdue, nextAt, nextPts, never));
        }

        List<PointsExpiryRow> filtered = new ArrayList<>();
        for (PointsExpiryRow x : result) {
            if (tierId != null && !tierId.equals(x.tierId())) {
                continue;
            }
            if ("30".equals(withinDays) && x.expiring30() <= 0) {
                continue;
            }
            if ("60".equals(withinDays) && x.expiring60() <= 0) {
                continue;
            }
            if ("90".equals(withinDays) && x.expiring90() <= 0) {
                continue;
            }
            filtered.add(x);
        }
        filtered.sort((x, y) -> {
            LocalDate xa = x.nextExpiryAt() == null ? LocalDate.of(9999, 12, 31) : x.nextExpiryAt();
            LocalDate ya = y.nextExpiryAt() == null ? LocalDate.of(9999, 12, 31) : y.nextExpiryAt();
            int byNext = xa.compareTo(ya);
            if (byNext != 0) {
                return byNext;
            }
            int by90 = Long.compare(y.expiring90(), x.expiring90());
            return by90 != 0 ? by90 : Long.compare(y.pointsBalance(), x.pointsBalance());
        });

        long total = filtered.size();
        int from = Math.min((int) pageable.getOffset(), filtered.size());
        int to = Math.min(from + pageable.getPageSize(), filtered.size());
        Page<PointsExpiryRow> page = new PageImpl<>(filtered.subList(from, to), pageable, total);

        PointsExpirySummary summary = new PointsExpirySummary(companyId, total,
            filtered.stream().mapToLong(PointsExpiryRow::pointsBalance).sum(),
            filtered.stream().mapToLong(PointsExpiryRow::expiring30).sum(),
            filtered.stream().mapToLong(PointsExpiryRow::expiring60).sum(),
            filtered.stream().mapToLong(PointsExpiryRow::expiring90).sum(),
            filtered.stream().mapToLong(PointsExpiryRow::overdue).sum());
        return new PointsExpiryResponse(PageResponse.of(page), summary);
    }

    private static LocalDate addMonths(LocalDate d, int months) {
        YearMonth ym = YearMonth.from(d).plusMonths(months);
        int day = Math.min(d.getDayOfMonth(), ym.lengthOfMonth());
        return ym.atDay(day);
    }
}

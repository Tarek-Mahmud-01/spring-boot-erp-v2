package com.guru.erp.modules.reporting.posloyalty.service;

import com.guru.erp.modules.crm.customers.domain.QCustomer;
import com.guru.erp.modules.crm.loyalty.domain.QLoyaltyLedger;
import com.guru.erp.modules.reporting.posloyalty.dto.LoyaltyReportDtos.LoyaltyLedgerResponse;
import com.guru.erp.modules.reporting.posloyalty.dto.LoyaltyReportDtos.LoyaltyLedgerRow;
import com.guru.erp.modules.reporting.posloyalty.dto.LoyaltyReportDtos.LoyaltyLedgerSummary;
import com.guru.erp.platform.web.PageResponse;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RPT-022 — Loyalty Ledger (reference {@code repositories/loyalty.py::loyalty_ledger}). Every
 * point movement with a true per-customer running balance, computed as a cumulative sum of ALL of
 * that customer's movements up to and including each row (unbounded by the date/kind filter — only
 * which rows are RETURNED changes; the running balance itself never does).
 *
 * <p>QueryDSL/JPQL has no portable window-function support, so the running balance is folded in
 * Java over the customer's full unfiltered history (bounded per-customer volume, matching the
 * reference's own note that this dataset is bounded by a single store's activity).
 */
@Service
@Transactional(readOnly = true)
public class LoyaltyLedgerQueryService {

    private final JPAQueryFactory queryFactory;
    private final PosLoyaltyReportSupport support;

    public LoyaltyLedgerQueryService(JPAQueryFactory queryFactory, PosLoyaltyReportSupport support) {
        this.queryFactory = queryFactory;
        this.support = support;
    }

    public LoyaltyLedgerResponse loyaltyLedger(String companyId, String customerId, LocalDate fromDate,
                                               LocalDate toDate, String kind, Pageable pageable) {
        support.validateDateRange(fromDate, toDate);

        QLoyaltyLedger ledger = QLoyaltyLedger.loyaltyLedger;
        QCustomer customer = QCustomer.customer;

        String custPublicId = null;
        if (customerId != null) {
            custPublicId = queryFactory.select(customer.publicId).from(customer)
                .where(customer.publicId.eq(customerId), customer.companyId.eq(companyId)).fetchOne();
            if (custPublicId == null) {
                return new LoyaltyLedgerResponse(PageResponse.of(Page.empty(pageable)),
                    new LoyaltyLedgerSummary(companyId, 0, 0, 0, 0));
            }
        }

        BooleanExpression scopePredicate = customer.companyId.eq(companyId);
        if (custPublicId != null) {
            scopePredicate = scopePredicate.and(ledger.customerId.eq(custPublicId));
        }

        // Full unfiltered history per matching customer(s), to compute the running balance.
        List<Tuple> allMovements = queryFactory
            .select(ledger.id, ledger.publicId, ledger.occurredAt, ledger.type, ledger.pointsSigned,
                ledger.sourceTransactionId, ledger.description, ledger.customerId, customer.membershipId,
                customer.firstName, customer.lastName)
            .from(ledger)
            .join(customer).on(customer.publicId.eq(ledger.customerId))
            .where(scopePredicate)
            .orderBy(ledger.customerId.asc(), ledger.occurredAt.asc(), ledger.id.asc())
            .fetch();

        String kindUpper = kind == null ? null : kind.toUpperCase(Locale.ROOT);
        java.time.Instant fromInstant = fromDate == null ? null : support.startOfDayUtc(fromDate);
        java.time.Instant toInstant = toDate == null ? null : support.endOfDayUtc(toDate);

        List<LoyaltyLedgerRow> filtered = new ArrayList<>();
        String currentCustomer = null;
        long running = 0L;
        for (Tuple t : allMovements) {
            String cust = t.get(ledger.customerId);
            if (!cust.equals(currentCustomer)) {
                currentCustomer = cust;
                running = 0L;
            }
            running += t.get(ledger.pointsSigned);

            var occurredAt = t.get(ledger.occurredAt);
            if (fromInstant != null && occurredAt.isBefore(fromInstant)) {
                continue;
            }
            if (toInstant != null && occurredAt.isAfter(toInstant)) {
                continue;
            }
            String movementKind = t.get(ledger.type).name();
            if (kindUpper != null && !movementKind.equals(kindUpper)) {
                continue;
            }
            String name = (t.get(customer.firstName) + " " + t.get(customer.lastName)).strip();
            filtered.add(new LoyaltyLedgerRow(
                t.get(ledger.publicId), occurredAt, t.get(customer.membershipId), name, movementKind,
                t.get(ledger.pointsSigned), running, t.get(ledger.sourceTransactionId), t.get(ledger.description)));
        }
        // Stable sort: rows already arrive in ascending (customerId, occurredAt, id) order from the
        // query above, so reversing that order also breaks occurredAt ties by descending id.
        java.util.Collections.reverse(filtered);
        filtered.sort((r1, r2) -> r2.occurredAt().compareTo(r1.occurredAt()));

        long total = filtered.size();
        int from = Math.min((int) pageable.getOffset(), filtered.size());
        int to = Math.min(from + pageable.getPageSize(), filtered.size());
        Page<LoyaltyLedgerRow> page = new PageImpl<>(filtered.subList(from, to), pageable, total);

        long earned = filtered.stream().mapToLong(r -> r.points() > 0 ? r.points() : 0).sum();
        long spent = filtered.stream().mapToLong(r -> r.points() < 0 ? -r.points() : 0).sum();
        LoyaltyLedgerSummary summary = new LoyaltyLedgerSummary(companyId, total, earned, spent, earned - spent);
        return new LoyaltyLedgerResponse(PageResponse.of(page), summary);
    }
}

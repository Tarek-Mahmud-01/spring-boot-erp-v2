package com.guru.erp.modules.reporting.posloyalty.service;

import com.guru.erp.modules.access.domain.QUser;
import com.guru.erp.modules.crm.customers.domain.QCustomer;
import com.guru.erp.modules.pos.auxiliary.domain.PosEventType;
import com.guru.erp.modules.pos.auxiliary.domain.QPosEvent;
import com.guru.erp.modules.pos.registers.domain.QRegister;
import com.guru.erp.modules.pos.transactions.domain.PosTransactionStatus;
import com.guru.erp.modules.pos.transactions.domain.QPosTransaction;
import com.guru.erp.modules.pos.transactions.domain.QPosTransactionLine;
import com.guru.erp.modules.reporting.posloyalty.dto.PosOpsReportDtos.DiscountResponse;
import com.guru.erp.modules.reporting.posloyalty.dto.PosOpsReportDtos.DiscountRow;
import com.guru.erp.modules.reporting.posloyalty.dto.PosOpsReportDtos.DiscountSummary;
import com.guru.erp.platform.web.PageResponse;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RPT-017 — Discount Usage (reference {@code repositories/pos_ops.py::discount_usage}).
 *
 * <p>Two independent sources folded into one report:
 * <ul>
 *   <li>Manual/manager-entered discounts — the {@code PosEvent} a completed manual-discount action
 *       writes ({@code MANUAL_DISCOUNT_APPLIED}), joined to its {@code PosTransaction} header.</li>
 *   <li>Promotional discounts — the per-line promotion stamp on {@code PosTransactionLine}
 *       ({@code discountAmount > 0}); this codebase's line entity carries no
 *       {@code appliedPromotionIds}/{@code promotionLabel} breakdown list, so (unlike the reference,
 *       which stacks multiple promo ids per line) one row is emitted per discounted sale line,
 *       attributed to its single {@code promotionLabel}.</li>
 * </ul>
 * Both are re-aggregated and paged in Java (small per-company/day volume, matching the reference's
 * own note that this dataset is bounded by a single store's daily volume).
 */
@Service
@Transactional(readOnly = true)
public class DiscountUsageQueryService {

    private static final PosTransactionStatus COMPLETED = PosTransactionStatus.COMPLETED;

    private final JPAQueryFactory queryFactory;
    private final PosLoyaltyReportSupport support;

    public DiscountUsageQueryService(JPAQueryFactory queryFactory, PosLoyaltyReportSupport support) {
        this.queryFactory = queryFactory;
        this.support = support;
    }

    public DiscountResponse discountUsage(String companyId, String locationId, String cashierId, String reason,
                                          String kind, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        support.validateDateRange(fromDate, toDate);
        String baseCurrency = support.baseCurrency(companyId);
        List<String> companyLocationIds = support.locationIdsForCompany(companyId);
        String kindUpper = kind == null ? null : kind.toUpperCase(java.util.Locale.ROOT);

        List<DiscountRow> rows = new ArrayList<>();
        if (!companyLocationIds.isEmpty()) {
            if (kindUpper == null || !kindUpper.equals("PROMOTION")) {
                rows.addAll(manualRows(companyLocationIds, locationId, cashierId, fromDate, toDate));
            }
            if (kindUpper == null || kindUpper.equals("PROMOTION")) {
                rows.addAll(promoRows(companyLocationIds, locationId, cashierId, fromDate, toDate));
            }
        }

        String reasonLower = reason == null ? null : reason.toLowerCase(java.util.Locale.ROOT);
        rows.removeIf(r -> (kindUpper != null && !r.kind().equalsIgnoreCase(kindUpper))
            || (reasonLower != null && !r.reason().toLowerCase(java.util.Locale.ROOT).contains(reasonLower)));
        rows.sort(Comparator.comparing(DiscountRow::occurredAt).reversed());

        long total = rows.size();
        int from = Math.min((int) pageable.getOffset(), rows.size());
        int to = Math.min(from + pageable.getPageSize(), rows.size());
        Page<DiscountRow> page = new PageImpl<>(rows.subList(from, to), pageable, total);

        long totalDiscount = rows.stream().mapToLong(r -> Math.abs(r.amountMinor())).sum();
        String currency = rows.isEmpty() ? baseCurrency : rows.get(0).currency();
        DiscountSummary summary = new DiscountSummary(companyId, total, totalDiscount, currency);
        return new DiscountResponse(PageResponse.of(page), summary);
    }

    private List<DiscountRow> manualRows(List<String> companyLocationIds, String locationId, String cashierId,
                                         LocalDate fromDate, LocalDate toDate) {
        QPosEvent event = QPosEvent.posEvent;
        QPosTransaction txn = QPosTransaction.posTransaction;
        QUser cashier = new QUser("discCashier");
        QCustomer customer = new QCustomer("discCustomer");

        BooleanExpression predicate = event.type.eq(PosEventType.MANUAL_DISCOUNT_APPLIED)
            .and(txn.status.eq(COMPLETED))
            .and(txn.locationId.in(companyLocationIds));
        if (locationId != null) {
            predicate = predicate.and(txn.locationId.eq(locationId));
        }
        if (cashierId != null) {
            predicate = predicate.and(cashier.publicId.eq(cashierId));
        }
        predicate = applyDateRange(predicate, event.createdAt, fromDate, toDate);

        List<Tuple> tuples = queryFactory
            .select(event.payload, event.createdAt, txn.publicId, txn.receiptNumber, txn.currency,
                txn.locationId, cashier.publicId, cashier.fullName, customer.firstName, customer.lastName)
            .from(event)
            .join(txn).on(txn.publicId.eq(event.transactionId))
            .leftJoin(cashier).on(cashier.publicId.eq(txn.cashierId))
            .leftJoin(customer).on(customer.publicId.eq(txn.customerId))
            .where(predicate)
            .fetch();

        List<DiscountRow> out = new ArrayList<>();
        for (Tuple t : tuples) {
            java.util.Map<String, Object> payload = t.get(event.payload);
            payload = payload == null ? java.util.Map.of() : payload;
            String custName = joinNonNull(t.get(customer.firstName), t.get(customer.lastName));
            Object amount = payload.get("amount");
            out.add(new DiscountRow(
                t.get(txn.publicId),
                t.get(txn.receiptNumber),
                t.get(event.createdAt),
                t.get(cashier.publicId),
                orDash(t.get(cashier.fullName)),
                t.get(txn.locationId),
                String.valueOf(payload.getOrDefault("kind", "MANUAL_FIXED")),
                amount == null ? 0L : ((Number) amount).longValue(),
                String.valueOf(payload.getOrDefault("reason", "")),
                (String) payload.get("manager_approval_name"),
                custName,
                t.get(txn.currency)));
        }
        return out;
    }

    private List<DiscountRow> promoRows(List<String> companyLocationIds, String locationId, String cashierId,
                                        LocalDate fromDate, LocalDate toDate) {
        QPosTransactionLine line = QPosTransactionLine.posTransactionLine;
        QPosTransaction txn = line.transaction;
        QUser cashier = new QUser("promoCashier");
        QCustomer customer = new QCustomer("promoCustomer");

        BooleanExpression predicate = txn.status.eq(COMPLETED)
            .and(txn.locationId.in(companyLocationIds))
            .and(line.discountAmount.gt(0L))
            .and(line.promotionLabel.isNotNull());
        if (locationId != null) {
            predicate = predicate.and(txn.locationId.eq(locationId));
        }
        if (cashierId != null) {
            predicate = predicate.and(cashier.publicId.eq(cashierId));
        }
        predicate = applyDateRange(predicate, txn.occurredAt, fromDate, toDate);

        List<Tuple> tuples = queryFactory
            .select(line.discountAmount, line.basePriceAmount, line.unitPriceAmount, line.qty, line.currency,
                line.promotionLabel, txn.occurredAt, txn.publicId, txn.receiptNumber, txn.locationId,
                cashier.publicId, cashier.fullName, customer.firstName, customer.lastName)
            .from(line)
            .leftJoin(cashier).on(cashier.publicId.eq(txn.cashierId))
            .leftJoin(customer).on(customer.publicId.eq(txn.customerId))
            .where(predicate)
            .fetch();

        List<DiscountRow> out = new ArrayList<>();
        for (Tuple t : tuples) {
            long promoFloor = Math.max(0L, orZero(t.get(line.basePriceAmount)) - orZero(t.get(line.unitPriceAmount)))
                * t.get(line.qty).longValue();
            long promoAmount = Math.min(promoFloor, orZero(t.get(line.discountAmount)));
            if (promoAmount <= 0) {
                continue;
            }
            String custName = joinNonNull(t.get(customer.firstName), t.get(customer.lastName));
            out.add(new DiscountRow(
                t.get(txn.publicId),
                t.get(txn.receiptNumber),
                t.get(txn.occurredAt),
                t.get(cashier.publicId),
                orDash(t.get(cashier.fullName)),
                t.get(txn.locationId),
                "PROMOTION",
                -promoAmount,
                t.get(line.promotionLabel),
                null,
                custName,
                t.get(line.currency)));
        }
        return out;
    }

    private static BooleanExpression applyDateRange(BooleanExpression predicate,
            com.querydsl.core.types.dsl.DateTimePath<Instant> path, LocalDate fromDate, LocalDate toDate) {
        BooleanExpression result = predicate;
        if (fromDate != null) {
            result = result.and(path.goe(fromDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant()));
        }
        if (toDate != null) {
            result = result.and(path.loe(toDate.atTime(23, 59, 59).atZone(java.time.ZoneOffset.UTC).toInstant()));
        }
        return result;
    }

    private static long orZero(Long v) {
        return v == null ? 0L : v;
    }

    private static String orDash(String v) {
        return v == null || v.isBlank() ? "—" : v;
    }

    private static String joinNonNull(String first, String last) {
        if (first == null && last == null) {
            return null;
        }
        String joined = ((first == null ? "" : first) + " " + (last == null ? "" : last)).strip();
        return joined.isEmpty() ? null : joined;
    }
}

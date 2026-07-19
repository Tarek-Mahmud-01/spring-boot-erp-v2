package com.guru.erp.modules.reporting.invproc.service;

import com.guru.erp.modules.procurement.bills.domain.BillStatus;
import com.guru.erp.modules.procurement.bills.domain.QSupplierBill;
import com.guru.erp.modules.procurement.orders.domain.PoStatus;
import com.guru.erp.modules.procurement.orders.domain.QPurchaseOrder;
import com.guru.erp.modules.procurement.receipts.domain.GrnStatus;
import com.guru.erp.modules.procurement.receipts.domain.QGoodsReceipt;
import com.guru.erp.modules.reporting.invproc.dto.InvProcReportDtos.PoAgingRow;
import com.guru.erp.modules.reporting.invproc.dto.InvProcReportDtos.PoAgingSummary;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PO / GRN / Bill Aging (RPT-E004-PO-AGING) — one row per {@link com.guru.erp.modules.procurement.orders.domain.PurchaseOrder}
 * with its latest linked {@link com.guru.erp.modules.procurement.receipts.domain.GoodsReceipt} and
 * {@link com.guru.erp.modules.procurement.bills.domain.SupplierBill}, and an aging bucket computed
 * from the bill's due date against {@code now}. PO/GRN/Bill are three separately-ported entities
 * linked only by a loose ULID (no cross-slice FK per the vertical-slice rule), so the "latest GRN" /
 * "latest bill" per PO are resolved via correlated subqueries keyed on {@code poId}, mirroring the
 * reference's 3-way document chain without a hard join.
 */
@Service
@Transactional(readOnly = true)
public class PoAgingQueryService {

    private final JPAQueryFactory queryFactory;
    private final ReportSupport support;

    public PoAgingQueryService(JPAQueryFactory queryFactory, ReportSupport support) {
        this.queryFactory = queryFactory;
        this.support = support;
    }

    public record Result(List<PoAgingRow> rows, PoAgingSummary summary, long total) {
    }

    public Result run(
        String supplierId,
        PoStatus poStatus,
        BillStatus billStatus,
        LocalDate fromDate,
        LocalDate toDate,
        String currency,
        Pageable pageable
    ) {
        support.validateRange(fromDate, toDate);
        QPurchaseOrder po = QPurchaseOrder.purchaseOrder;
        QGoodsReceipt grn = QGoodsReceipt.goodsReceipt;
        QSupplierBill bill = QSupplierBill.supplierBill;

        // Latest GRN and latest bill per PO — correlated subqueries on the loose poId reference.
        var latestGrnId = JPAExpressions.select(grn.id.max()).from(grn).where(grn.poId.eq(po.publicId));
        var latestBillId = JPAExpressions.select(bill.id.max()).from(bill).where(bill.poId.eq(po.publicId));

        QGoodsReceipt g = new QGoodsReceipt("g");
        QSupplierBill b = new QSupplierBill("b");

        List<BooleanExpression> conds = new ArrayList<>();
        if (supplierId != null) {
            conds.add(po.supplierId.eq(supplierId));
        }
        if (poStatus != null) {
            conds.add(po.status.eq(poStatus));
        }
        if (billStatus != null) {
            conds.add(b.status.eq(billStatus));
        }
        if (fromDate != null) {
            conds.add(po.poDate.goe(support.startOfDay(fromDate)));
        }
        if (toDate != null) {
            conds.add(po.poDate.loe(support.endOfDay(toDate)));
        }
        BooleanExpression where = support.and(conds);

        var baseQuery = queryFactory
            .select(po.publicId, po.number, po.status, po.supplierId, po.poDate,
                g.publicId, g.number, g.status, g.receivedAt,
                b.publicId, b.number, b.status, b.dueDate, b.totalAmount)
            .from(po)
            .leftJoin(g).on(g.id.eq(latestGrnId))
            .leftJoin(b).on(b.id.eq(latestBillId))
            .where(where);

        long total = baseQuery.fetchCount();

        List<Tuple> pageRows = baseQuery
            .orderBy(po.poDate.desc(), po.id.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Instant now = Instant.now();
        List<PoAgingRow> rows = new ArrayList<>();
        long current = 0L;
        long b1To30 = 0L;
        long b31To60 = 0L;
        long b61To90 = 0L;
        long b90Plus = 0L;
        long totalOutstanding = 0L;
        for (Tuple t : pageRows) {
            Instant dueDate = t.get(b.dueDate);
            long billTotal = t.get(b.totalAmount) == null ? 0L : t.get(b.totalAmount);
            long daysOverdue = 0L;
            String bucket = "N/A";
            BillStatus bs = t.get(b.status);
            boolean settled = bs == BillStatus.PAID || bs == BillStatus.CANCELLED;
            if (dueDate != null && !settled) {
                daysOverdue = ChronoUnit.DAYS.between(dueDate, now);
                bucket = bucketFor(daysOverdue);
                totalOutstanding += billTotal;
                switch (bucket) {
                    case "CURRENT" -> current += billTotal;
                    case "1-30" -> b1To30 += billTotal;
                    case "31-60" -> b31To60 += billTotal;
                    case "61-90" -> b61To90 += billTotal;
                    default -> b90Plus += billTotal;
                }
            }
            rows.add(new PoAgingRow(
                t.get(po.publicId), t.get(po.number), wire(t.get(po.status)), t.get(po.supplierId), t.get(po.poDate),
                t.get(g.publicId), t.get(g.number), wire(t.get(g.status)), t.get(g.receivedAt),
                t.get(b.publicId), t.get(b.number), wire(t.get(b.status)), dueDate, billTotal,
                currency, daysOverdue, bucket
            ));
        }

        PoAgingSummary summary = new PoAgingSummary(currency, total, totalOutstanding, current, b1To30, b31To60, b61To90, b90Plus);
        return new Result(rows, summary, total);
    }

    private static String bucketFor(long daysOverdue) {
        if (daysOverdue <= 0) {
            return "CURRENT";
        }
        if (daysOverdue <= 30) {
            return "1-30";
        }
        if (daysOverdue <= 60) {
            return "31-60";
        }
        if (daysOverdue <= 90) {
            return "61-90";
        }
        return "90+";
    }

    private static String wire(PoStatus s) {
        return s == null ? null : s.wire();
    }

    private static String wire(GrnStatus s) {
        return s == null ? null : s.wire();
    }

    private static String wire(BillStatus s) {
        return s == null ? null : s.wire();
    }
}

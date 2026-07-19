package com.guru.erp.modules.reporting.invproc.service;

import com.guru.erp.modules.procurement.orders.domain.QPurchaseOrder;
import com.guru.erp.modules.procurement.orders.domain.QPurchaseOrderLine;
import com.guru.erp.modules.procurement.suppliers.domain.QSupplier;
import com.guru.erp.modules.reporting.invproc.dto.InvProcReportDtos.SupplierSpendRow;
import com.guru.erp.modules.reporting.invproc.dto.InvProcReportDtos.SupplierSpendSummary;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Supplier Spend (RPT-E004-SUPPLIER-SPEND) — {@code Σ purchase_order_lines.line_total_amount} per
 * supplier over an optional {@code [fromDate, toDate]} window on {@code PurchaseOrder.poDate}, plus
 * a PO count. A single GROUP BY over the joined {@code Supplier -> PurchaseOrder -> PurchaseOrderLine}
 * chain — no per-supplier loop.
 */
@Service
@Transactional(readOnly = true)
public class SupplierSpendQueryService {

    private final JPAQueryFactory queryFactory;
    private final ReportSupport support;

    public SupplierSpendQueryService(JPAQueryFactory queryFactory, ReportSupport support) {
        this.queryFactory = queryFactory;
        this.support = support;
    }

    public record Result(List<SupplierSpendRow> rows, SupplierSpendSummary summary, long total) {
    }

    public Result run(LocalDate fromDate, LocalDate toDate, String currency, Pageable pageable) {
        support.validateRange(fromDate, toDate);
        QSupplier s = QSupplier.supplier;
        QPurchaseOrder po = QPurchaseOrder.purchaseOrder;
        QPurchaseOrderLine line = QPurchaseOrderLine.purchaseOrderLine;

        List<BooleanExpression> poConds = new ArrayList<>();
        poConds.add(po.supplierId.eq(s.publicId));
        Instant fromDt = support.startOfDay(fromDate);
        Instant toDt = support.endOfDay(toDate);
        if (fromDt != null) {
            poConds.add(po.poDate.goe(fromDt));
        }
        if (toDt != null) {
            poConds.add(po.poDate.loe(toDt));
        }

        List<Tuple> grouped = queryFactory
            .select(s.publicId, s.code, s.name, po.countDistinct(), line.lineTotal.amountMinor.sum())
            .from(s)
            .leftJoin(po).on(support.and(poConds))
            .leftJoin(line).on(line.purchaseOrder.id.eq(po.id))
            .groupBy(s.id, s.publicId, s.code, s.name)
            .having(po.countDistinct().gt(0L))
            .fetch();

        grouped.sort((a, b) -> a.get(s.code).compareTo(b.get(s.code)));

        long total = grouped.size();
        int from = Math.min(pageable.getPageNumber() * pageable.getPageSize(), grouped.size());
        int to = Math.min(from + pageable.getPageSize(), grouped.size());

        List<SupplierSpendRow> rows = new ArrayList<>();
        long totalSpend = 0L;
        for (Tuple t : grouped) {
            totalSpend += nzLong(t.get(line.lineTotal.amountMinor.sum()));
        }
        for (Tuple t : grouped.subList(from, to)) {
            rows.add(new SupplierSpendRow(
                t.get(s.publicId), t.get(s.code), t.get(s.name),
                t.get(po.countDistinct()) == null ? 0L : t.get(po.countDistinct()),
                nzLong(t.get(line.lineTotal.amountMinor.sum())), currency));
        }

        SupplierSpendSummary summary = new SupplierSpendSummary(currency, grouped.size(), totalSpend);
        return new Result(rows, summary, total);
    }

    private static long nzLong(Long v) {
        return v == null ? 0L : v;
    }
}

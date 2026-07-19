package com.guru.erp.modules.reporting.invproc.service;

import com.guru.erp.modules.inventory.movements.domain.QStockTransfer;
import com.guru.erp.modules.inventory.movements.domain.QStockTransferLine;
import com.guru.erp.modules.inventory.movements.domain.TransferStatus;
import com.guru.erp.modules.inventory.stock.domain.MovementType;
import com.guru.erp.modules.inventory.stock.domain.QStockLedger;
import com.guru.erp.modules.inventory.stock.domain.SourceDocType;
import com.guru.erp.modules.reporting.invproc.dto.InvProcReportDtos.TransferLineRow;
import com.guru.erp.modules.reporting.invproc.dto.InvProcReportDtos.TransferSummary;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stock Transfer Register (RPT-E005-TRANSFER) — variant-aware transfer lines (reference
 * {@code transfer_register_report}). {@link com.guru.erp.modules.inventory.movements.domain.StockTransferLine}
 * has no {@code variant_id} column, so the variant is resolved via a correlated scalar subquery on
 * {@link com.guru.erp.modules.inventory.stock.domain.StockLedger}: the destination
 * {@code TRANSFER_IN} row carries the variant and is keyed by
 * {@code source_doc_id = transfer.publicId} + {@code source_doc_type = TRANSFER} +
 * {@code product_id}. Status carries a backend-owned {@code StatusPill} tone so the frontend never
 * hardcodes a status-&gt;colour map.
 */
@Service
@Transactional(readOnly = true)
public class TransferRegisterQueryService {

    private static final Map<TransferStatus, String> STATUS_TONE = Map.of(
        TransferStatus.DRAFT, "draft",
        TransferStatus.APPROVED, "approved",
        TransferStatus.PARTIALLY_COMPLETE, "warning",
        TransferStatus.COMPLETE, "success"
    );

    private final JPAQueryFactory queryFactory;
    private final ReportSupport support;

    public TransferRegisterQueryService(JPAQueryFactory queryFactory, ReportSupport support) {
        this.queryFactory = queryFactory;
        this.support = support;
    }

    public record Result(List<TransferLineRow> rows, TransferSummary summary, long total) {
    }

    public Result run(
        String fromLocationId,
        String toLocationId,
        String productId,
        TransferStatus status,
        LocalDate fromDate,
        LocalDate toDate,
        Pageable pageable
    ) {
        support.validateRange(fromDate, toDate);
        QStockTransfer t = QStockTransfer.stockTransfer;
        QStockTransferLine line = QStockTransferLine.stockTransferLine;

        List<BooleanExpression> conds = new ArrayList<>();
        if (fromLocationId != null) {
            conds.add(t.sourceLocationId.eq(fromLocationId));
        }
        if (toLocationId != null) {
            conds.add(t.destinationLocationId.eq(toLocationId));
        }
        if (productId != null) {
            conds.add(line.productId.eq(productId));
        }
        if (status != null) {
            conds.add(t.status.eq(status));
        }
        if (fromDate != null) {
            conds.add(t.createdAt.goe(support.startOfDay(fromDate)));
        }
        if (toDate != null) {
            conds.add(t.createdAt.loe(support.endOfDay(toDate)));
        }
        BooleanExpression where = support.and(conds);

        long total = queryFactory
            .select(line.count())
            .from(line)
            .join(t).on(t.id.eq(line.transfer.id))
            .where(where)
            .fetchOne();

        // Variant resolved per (transfer, product) via a correlated subquery on StockLedger's
        // destination TRANSFER_IN row — StockTransferLine itself carries no variant_id column.
        QStockLedger sl = QStockLedger.stockLedger;
        var variantSubquery = JPAExpressions
            .select(sl.variantId.min())
            .from(sl)
            .where(sl.sourceDocId.eq(t.publicId)
                .and(sl.sourceDocType.eq(SourceDocType.TRANSFER))
                .and(sl.productId.eq(line.productId))
                .and(sl.movementType.eq(MovementType.TRANSFER_IN)));

        List<Tuple> pageRows = queryFactory
            .select(t.createdAt, t.number, t.status, t.sourceLocationId, t.destinationLocationId,
                line.productId, variantSubquery, line.qtySent, line.qtyReceived)
            .from(line)
            .join(t).on(t.id.eq(line.transfer.id))
            .where(where)
            .orderBy(t.createdAt.desc(), line.id.asc())
            .offset((long) pageable.getPageNumber() * pageable.getPageSize())
            .limit(pageable.getPageSize())
            .fetch();

        List<TransferLineRow> rows = new ArrayList<>();
        for (Tuple r : pageRows) {
            TransferStatus st = r.get(2, TransferStatus.class);
            rows.add(new TransferLineRow(
                r.get(0, java.time.Instant.class),
                r.get(1, String.class),
                st != null ? st.wire() : null,
                st != null ? STATUS_TONE.getOrDefault(st, "draft") : "draft",
                r.get(3, String.class),
                r.get(4, String.class),
                r.get(5, String.class),
                r.get(6, String.class),
                support.nz(r.get(7, BigDecimal.class)).doubleValue(),
                support.nz(r.get(8, BigDecimal.class)).doubleValue()
            ));
        }

        Tuple totals = queryFactory
            .select(line.qtySent.sum(), line.qtyReceived.sum())
            .from(line)
            .join(t).on(t.id.eq(line.transfer.id))
            .where(where)
            .fetchOne();
        double totalSent = totals == null ? 0d : support.nz(totals.get(0, BigDecimal.class)).doubleValue();
        double totalReceived = totals == null ? 0d : support.nz(totals.get(1, BigDecimal.class)).doubleValue();

        TransferSummary summary = new TransferSummary(total, totalSent, totalReceived);
        return new Result(rows, summary, total);
    }
}

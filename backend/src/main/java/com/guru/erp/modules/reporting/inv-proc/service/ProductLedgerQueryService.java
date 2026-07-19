package com.guru.erp.modules.reporting.invproc.service;

import com.guru.erp.modules.inventory.stock.domain.MovementType;
import com.guru.erp.modules.inventory.stock.domain.QStockLedger;
import com.guru.erp.modules.inventory.stock.domain.SourceDocType;
import com.guru.erp.modules.inventory.stock.domain.StockLedger;
import com.guru.erp.modules.inventory.stock.domain.StockStatus;
import com.guru.erp.modules.reporting.invproc.dto.InvProcReportDtos.ProductLedgerRow;
import com.guru.erp.modules.reporting.invproc.dto.InvProcReportDtos.ProductLedgerSummary;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product Ledger (RPT-E005-PROD-LEDGER) — chronological {@link StockLedger} rows for one product
 * with a running qty. A stock transfer parks goods in an {@code IN_TRANSIT} staging bucket on
 * confirm (a {@code +TRANSFER_IN} row) and clears it on receive (a {@code -TRANSFER_IN} row), then
 * books the goods into {@code AVAILABLE} as the destination movement — those two {@code IN_TRANSIT}
 * rows are internal double-entry plumbing and are hidden from the ledger (so one transfer reads as
 * one line) unless the caller explicitly filters to that movement type. {@code closingQty} always
 * uses the unfiltered net so on-hand ties to Stock Summary.
 */
@Service
@Transactional(readOnly = true)
public class ProductLedgerQueryService {

    private final JPAQueryFactory queryFactory;
    private final ReportSupport support;

    public ProductLedgerQueryService(JPAQueryFactory queryFactory, ReportSupport support) {
        this.queryFactory = queryFactory;
        this.support = support;
    }

    public record Result(List<ProductLedgerRow> rows, ProductLedgerSummary summary, long total) {
    }

    public Result run(
        String productId,
        String variantId,
        String locationId,
        LocalDate fromDate,
        LocalDate toDate,
        MovementType movementType,
        String currency,
        Pageable pageable
    ) {
        support.validateRange(fromDate, toDate);
        QStockLedger l = QStockLedger.stockLedger;

        List<BooleanExpression> baseConds = new ArrayList<>();
        baseConds.add(l.productId.eq(productId));
        if (locationId != null) {
            baseConds.add(l.locationId.eq(locationId));
        }
        if (variantId != null) {
            baseConds.add(l.variantId.eq(variantId));
        }
        if (movementType != null) {
            baseConds.add(l.movementType.eq(movementType));
        }

        Instant fromDt = support.startOfDay(fromDate);
        Instant toDt = support.endOfDay(toDate);

        double openingQty = fromDt == null ? 0d : openingQty(l, baseConds, fromDt);

        List<BooleanExpression> periodConds = new ArrayList<>(baseConds);
        if (fromDt != null) {
            periodConds.add(l.occurredAt.goe(fromDt));
        }
        if (toDt != null) {
            periodConds.add(l.occurredAt.loe(toDt));
        }

        BooleanExpression availIo = l.movementType.ne(MovementType.TRANSFER_IN);
        Tuple periodTotals = queryFactory
            .select(
                com.querydsl.core.types.dsl.Expressions.cases()
                    .when(l.qtySigned.gt(BigDecimal.ZERO).and(availIo)).then(l.qtySigned)
                    .otherwise(BigDecimal.ZERO).sum(),
                com.querydsl.core.types.dsl.Expressions.cases()
                    .when(l.qtySigned.lt(BigDecimal.ZERO).and(availIo)).then(l.qtySigned)
                    .otherwise(BigDecimal.ZERO).sum(),
                l.qtySigned.sum())
            .from(l)
            .where(support.and(periodConds))
            .fetchOne();
        double periodIn = periodTotals == null ? 0d : support.nz(periodTotals.get(0, BigDecimal.class)).doubleValue();
        double periodOut = periodTotals == null ? 0d : Math.abs(support.nz(periodTotals.get(1, BigDecimal.class)).doubleValue());
        double netQty = periodTotals == null ? 0d : support.nz(periodTotals.get(2, BigDecimal.class)).doubleValue();
        double closingQty = openingQty + netQty;

        List<BooleanExpression> rowConds = new ArrayList<>(periodConds);
        if (movementType == null) {
            rowConds.add(l.status.ne(StockStatus.IN_TRANSIT));
        }
        BooleanExpression rowWhere = support.and(rowConds);

        long total = queryFactory.select(l.count()).from(l).where(rowWhere).fetchOne();

        // Fetch every matching row up to (and including) the requested page, ordered chronologically,
        // and fold a running total across them — equivalent to the reference's windowed
        // SUM() OVER(ORDER BY occurred_at, id) restricted to this page.
        long upToRow = (long) (pageable.getPageNumber() + 1) * pageable.getPageSize();
        List<StockLedger> upTo = queryFactory
            .selectFrom(l)
            .where(rowWhere)
            .orderBy(l.occurredAt.asc(), l.id.asc())
            .limit(upToRow)
            .fetch();

        long skip = (long) pageable.getPageNumber() * pageable.getPageSize();
        List<ProductLedgerRow> rows = new ArrayList<>();
        double running = openingQty;
        for (int i = 0; i < upTo.size(); i++) {
            StockLedger e = upTo.get(i);
            running += e.getQtySigned().doubleValue();
            if (i >= skip) {
                rows.add(new ProductLedgerRow(
                    e.getPublicId(),
                    e.getOccurredAt(),
                    e.getMovementType().name(),
                    e.getLocationId(),
                    e.getVariantId(),
                    e.getQtySigned().doubleValue(),
                    e.getUnitCost() != null ? e.getUnitCost().amountMinor() : 0L,
                    e.getUnitCost() != null ? e.getUnitCost().currency() : currency,
                    e.getSourceDocType() != null ? e.getSourceDocType().name() : null,
                    e.getSourceDocId(),
                    running,
                    e.getNotes()
                ));
            }
        }

        long closingValue = closingValue(l, productId, locationId, variantId, toDt, closingQty);
        ProductLedgerSummary summary = new ProductLedgerSummary(
            productId, currency, openingQty, periodIn, periodOut, closingQty, closingValue, total);
        return new Result(rows, summary, total);
    }

    private double openingQty(QStockLedger l, List<BooleanExpression> baseConds, Instant fromDt) {
        List<BooleanExpression> openConds = new ArrayList<>(baseConds);
        openConds.add(l.occurredAt.lt(fromDt));
        BigDecimal opening = queryFactory.select(l.qtySigned.sum()).from(l).where(support.and(openConds)).fetchOne();
        return support.nz(opening).doubleValue();
    }

    /** Weighted-avg cost basis (RECEIPT/RETURN/REVALUATION + opening-balance rows), capped at toDate. */
    private long closingValue(QStockLedger l, String productId, String locationId, String variantId, Instant toDt, double closingQty) {
        List<BooleanExpression> macConds = new ArrayList<>();
        macConds.add(l.productId.eq(productId));
        macConds.add(l.movementType.in(MovementType.RECEIPT, MovementType.RETURN, MovementType.REVALUATION)
            .or(l.sourceDocType.eq(SourceDocType.STOCK_OPENING)));
        if (toDt != null) {
            macConds.add(l.occurredAt.loe(toDt));
        }
        if (locationId != null) {
            macConds.add(l.locationId.eq(locationId));
        }
        if (variantId != null) {
            macConds.add(l.variantId.eq(variantId));
        }
        Tuple macRow = queryFactory
            .select(l.qtySigned.sum(),
                l.qtySigned.multiply(l.unitCost.amountMinor).add(l.valueDeltaAmount).sum())
            .from(l)
            .where(support.and(macConds))
            .fetchOne();
        BigDecimal macQty = macRow == null ? BigDecimal.ZERO : support.nz(macRow.get(0, BigDecimal.class));
        BigDecimal macVal = macRow == null ? BigDecimal.ZERO : support.nz(macRow.get(1, BigDecimal.class));
        BigDecimal avgUnit = support.avg(macVal, macQty);
        return support.roundMinor(BigDecimal.valueOf(closingQty).multiply(avgUnit));
    }
}

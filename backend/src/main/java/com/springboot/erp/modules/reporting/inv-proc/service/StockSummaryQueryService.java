package com.springboot.erp.modules.reporting.invproc.service;

import com.springboot.erp.modules.inventory.stock.domain.MovementType;
import com.springboot.erp.modules.inventory.stock.domain.QStockLedger;
import com.springboot.erp.modules.inventory.stock.domain.SourceDocType;
import com.springboot.erp.modules.reporting.invproc.dto.InvProcReportDtos.StockSummaryRow;
import com.springboot.erp.modules.reporting.invproc.dto.InvProcReportDtos.StockSummarySummary;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
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
 * Stock Summary (RPT-E005-STOCK-SUM) — one {@code SUM(qty_signed)} GROUP BY rolls EVERY
 * {@link com.springboot.erp.modules.inventory.stock.domain.StockLedger} movement type (RECEIPT / SALE /
 * RETURN / TRANSFER_IN / TRANSFER_OUT / ADJUSTMENT / WRITE_OFF / RESERVE) up into on-hand qty per
 * (product, variant, location) — the reference's "single-pass, no per-movement-type loop"
 * aggregation (reference {@code test_stock_summary_all_movements.py}).
 *
 * <p>{@code IN_TRANSIT} {@code TRANSFER_IN} staging rows are excluded from the IN/OUT breakdown
 * columns (a transfer writes a +staging row on confirm and a -reversal row on receive; counting
 * both would double the transferred qty in IN and OUT while leaving on-hand unaffected) — the
 * destination inbound is the RECEIPT-equivalent row and the source TRANSFER_OUT still counts.
 * On-hand / opening always use the UNFILTERED net so this report ties to the ledger's true balance.
 */
@Service
@Transactional(readOnly = true)
public class StockSummaryQueryService {

    private final JPAQueryFactory queryFactory;
    private final ReportSupport support;

    public StockSummaryQueryService(JPAQueryFactory queryFactory, ReportSupport support) {
        this.queryFactory = queryFactory;
        this.support = support;
    }

    public record Result(List<StockSummaryRow> rows, StockSummarySummary summary, long total) {
    }

    public Result run(
        String locationId,
        String productId,
        String variantId,
        boolean onlyInStock,
        LocalDate asOfDate,
        LocalDate inFrom,
        LocalDate inTo,
        String currency,
        Pageable pageable
    ) {
        support.validateRange(inFrom, inTo);
        QStockLedger l = QStockLedger.stockLedger;

        List<BooleanExpression> conds = new ArrayList<>();
        if (locationId != null) {
            conds.add(l.locationId.eq(locationId));
        }
        if (productId != null) {
            conds.add(l.productId.eq(productId));
        }
        if (variantId != null) {
            conds.add(l.variantId.eq(variantId));
        }
        if (asOfDate != null) {
            conds.add(l.occurredAt.loe(support.endOfDay(asOfDate)));
        }

        Instant inFromDt = support.startOfDay(inFrom);
        Instant inToDt = support.endOfDay(inTo);
        BooleanExpression availIo = l.movementType.ne(MovementType.TRANSFER_IN);
        BooleanExpression window = windowPredicate(l, inFromDt, inToDt);

        NumberExpression<BigDecimal> onHandExpr = l.qtySigned.sum().as("onHand");
        NumberExpression<BigDecimal> inQtyExpr = Expressions.cases()
            .when(l.qtySigned.gt(BigDecimal.ZERO).and(availIo).and(window)).then(l.qtySigned)
            .otherwise(BigDecimal.ZERO).sum().as("inQty");
        NumberExpression<BigDecimal> outQtyExpr = Expressions.cases()
            .when(l.qtySigned.lt(BigDecimal.ZERO).and(availIo).and(window)).then(l.qtySigned.negate())
            .otherwise(BigDecimal.ZERO).sum().as("outQty");
        NumberExpression<BigDecimal> openingExpr = (inFromDt == null
            ? Expressions.asNumber(BigDecimal.ZERO)
            : Expressions.cases().when(l.occurredAt.lt(inFromDt)).then(l.qtySigned)
                .otherwise(BigDecimal.ZERO).sum()).as("opening");

        // Weighted-avg cost basis: RECEIPT/RETURN/REVALUATION rows + opening-balance rows
        // (ADJUSTMENT with source_doc_type=STOCK_OPENING) — mirrors the moving-average cost the
        // stock posting service itself computes, so this report agrees with GL inventory value.
        BooleanExpression costBasis = l.movementType.in(MovementType.RECEIPT, MovementType.RETURN, MovementType.REVALUATION)
            .or(l.sourceDocType.eq(SourceDocType.STOCK_OPENING));
        NumberExpression<BigDecimal> posQty = Expressions.cases().when(costBasis).then(l.qtySigned)
            .otherwise(BigDecimal.ZERO).sum().as("posQty");
        NumberPath<Long> unitCostMinor = l.unitCost.amountMinor;
        NumberExpression<BigDecimal> posVal = Expressions.cases().when(costBasis)
            .then(l.qtySigned.multiply(unitCostMinor)
                .add(Expressions.numberTemplate(BigDecimal.class, "{0}", l.valueDeltaAmount)))
            .otherwise(BigDecimal.ZERO).sum().as("posVal");

        List<Tuple> grouped = queryFactory
            .select(l.productId, l.variantId, l.locationId, onHandExpr, inQtyExpr, outQtyExpr, openingExpr, posQty, posVal)
            .from(l)
            .where(support.and(conds))
            .groupBy(l.productId, l.variantId, l.locationId)
            .fetch();

        List<Tuple> filtered = new ArrayList<>();
        for (Tuple t : grouped) {
            BigDecimal onHand = t.get(onHandExpr);
            if (!onlyInStock || (onHand != null && onHand.signum() > 0)) {
                filtered.add(t);
            }
        }

        long total = filtered.size();
        int from = Math.min(pageable.getPageNumber() * pageable.getPageSize(), filtered.size());
        int to = Math.min(from + pageable.getPageSize(), filtered.size());

        StockSummarySummary summary = summarize(filtered, currency, onHandExpr, inQtyExpr, outQtyExpr, openingExpr, posQty, posVal);
        List<StockSummaryRow> rows = new ArrayList<>();
        for (Tuple t : filtered.subList(from, to)) {
            rows.add(toRow(t, currency, onHandExpr, inQtyExpr, outQtyExpr, openingExpr, posQty, posVal, l));
        }
        return new Result(rows, summary, total);
    }

    private StockSummaryRow toRow(Tuple t, String currency, NumberExpression<BigDecimal> onHandExpr,
                                   NumberExpression<BigDecimal> inQtyExpr, NumberExpression<BigDecimal> outQtyExpr,
                                   NumberExpression<BigDecimal> openingExpr, NumberExpression<BigDecimal> posQty,
                                   NumberExpression<BigDecimal> posVal, QStockLedger l) {
        BigDecimal onHand = support.nz(t.get(onHandExpr));
        BigDecimal avgFull = support.avg(t.get(posVal), t.get(posQty));
        long avg = support.roundMinor(avgFull);
        long value = support.roundMinor(onHand.multiply(avgFull));
        return new StockSummaryRow(
            t.get(l.productId),
            t.get(l.variantId),
            t.get(l.locationId),
            onHand.doubleValue(),
            support.nz(t.get(openingExpr)).doubleValue(),
            support.nz(t.get(inQtyExpr)).doubleValue(),
            support.nz(t.get(outQtyExpr)).doubleValue(),
            avg,
            value,
            currency
        );
    }

    /** Company-wide rollup: sum every bucket's own HALF_EVEN-rounded value so header == Σ(rows). */
    private StockSummarySummary summarize(List<Tuple> filtered, String currency, NumberExpression<BigDecimal> onHandExpr,
                                           NumberExpression<BigDecimal> inQtyExpr, NumberExpression<BigDecimal> outQtyExpr,
                                           NumberExpression<BigDecimal> openingExpr, NumberExpression<BigDecimal> posQty,
                                           NumberExpression<BigDecimal> posVal) {
        double totalQty = 0d;
        double opening = 0d;
        double in = 0d;
        double out = 0d;
        long totalValue = 0L;
        for (Tuple t : filtered) {
            BigDecimal onHand = support.nz(t.get(onHandExpr));
            totalQty += onHand.doubleValue();
            opening += support.nz(t.get(openingExpr)).doubleValue();
            in += support.nz(t.get(inQtyExpr)).doubleValue();
            out += support.nz(t.get(outQtyExpr)).doubleValue();
            BigDecimal avgFull = support.avg(t.get(posVal), t.get(posQty));
            totalValue += support.roundMinor(onHand.multiply(avgFull));
        }
        return new StockSummarySummary(currency, filtered.size(), totalQty, opening, in, out, totalValue);
    }

    private BooleanExpression windowPredicate(QStockLedger l, Instant from, Instant to) {
        BooleanExpression expr = null;
        if (from != null) {
            expr = l.occurredAt.goe(from);
        }
        if (to != null) {
            expr = expr == null ? l.occurredAt.loe(to) : expr.and(l.occurredAt.loe(to));
        }
        return expr == null ? Expressions.TRUE.isTrue() : expr;
    }
}

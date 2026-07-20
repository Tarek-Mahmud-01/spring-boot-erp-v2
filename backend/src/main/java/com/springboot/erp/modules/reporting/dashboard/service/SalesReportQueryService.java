package com.springboot.erp.modules.reporting.dashboard.service;

import com.springboot.erp.modules.pos.transactions.domain.PosTransaction;
import com.springboot.erp.modules.pos.transactions.domain.PosTransactionStatus;
import com.springboot.erp.modules.pos.transactions.domain.PosTransactionType;
import com.springboot.erp.modules.pos.transactions.domain.QPosTransaction;
import com.springboot.erp.modules.pos.transactions.domain.QPosTransactionLine;
import com.springboot.erp.modules.product.catalog.domain.QProduct;
import com.springboot.erp.modules.reporting.dashboard.dto.DashboardDtos.SalesByProductRow;
import com.springboot.erp.modules.reporting.dashboard.dto.DashboardDtos.SalesByProductSummary;
import com.springboot.erp.modules.reporting.dashboard.dto.DashboardDtos.TransactionRow;
import com.springboot.erp.modules.reporting.dashboard.dto.DashboardDtos.TransactionsSummary;
import com.springboot.erp.modules.reporting.dashboard.dto.DashboardDtos.TxnLineRow;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.web.PageResponse;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RPT-015 Sales by Product + RPT-AU-TXN POS transaction register (reference
 * {@code sales.sales_by_product} / {@code pos_txn.report_transactions}). Both aggregate/join
 * entirely in SQL via QueryDSL (group-by + sum, or one ordered fetch of lines batched by
 * transaction id) — no per-row N+1. Gross revenue is {@code lineNetAmount} (discounted,
 * tax-exclusive), matching the reference convention so every report's revenue reconciles.
 */
@Service
@Transactional(readOnly = true)
public class SalesReportQueryService {

    private static final QPosTransaction TXN = QPosTransaction.posTransaction;
    private static final QPosTransactionLine LINE = QPosTransactionLine.posTransactionLine;
    private static final QProduct PRODUCT = QProduct.product;

    private final JPAQueryFactory query;

    public SalesReportQueryService(JPAQueryFactory query) {
        this.query = query;
    }

    /** RPT-015 — Sales by Product, grouped by product, paged, gross-descending. */
    public PageResponse<SalesByProductRow> salesByProduct(String locationId, LocalDate fromDate,
                                                          LocalDate toDate, Pageable pageable) {
        validateRange(fromDate, toDate);
        BooleanExpression where = saleConds(locationId, fromDate, toDate);

        long total = query.select(LINE.productId.countDistinct())
            .from(LINE)
            .join(LINE.transaction, TXN)
            .where(where)
            .fetchOne();

        List<Tuple> rows = query.select(
                LINE.productId,
                LINE.sku.min(),
                PRODUCT.name.min().coalesce(LINE.name.min()),
                LINE.qty.sum().add(LINE.freeQty.sum()),
                LINE.lineNetAmount.sum(),
                LINE.taxAmount.sum(),
                TXN.id.countDistinct(),
                TXN.occurredAt.max(),
                LINE.currency.min())
            .from(LINE)
            .join(LINE.transaction, TXN)
            .leftJoin(PRODUCT).on(PRODUCT.publicId.eq(LINE.productId))
            .where(where)
            .groupBy(LINE.productId)
            .orderBy(LINE.lineNetAmount.sum().desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        List<SalesByProductRow> content = rows.stream()
            .map(t -> {
                long gross = nz(t.get(4, Long.class));
                return new SalesByProductRow(
                    t.get(0, String.class),
                    t.get(1, String.class),
                    t.get(2, String.class),
                    qtyOf(t.get(3, BigDecimal.class)),
                    gross,
                    nz(t.get(5, Long.class)),
                    gross,
                    t.get(8, String.class),
                    nz(t.get(6, Long.class)),
                    t.get(7, Instant.class));
            })
            .toList();

        return page(content, pageable, total);
    }

    /** Aggregate roll-up (product_count/total_qty/total_gross/tax/net) for the same filter set. */
    public SalesByProductSummary salesByProductSummary(String locationId, LocalDate fromDate, LocalDate toDate) {
        validateRange(fromDate, toDate);
        BooleanExpression where = saleConds(locationId, fromDate, toDate);
        Tuple roll = query.select(
                LINE.productId.countDistinct(),
                LINE.qty.sum().add(LINE.freeQty.sum()),
                LINE.lineNetAmount.sum(),
                LINE.taxAmount.sum(),
                LINE.currency.min())
            .from(LINE)
            .join(LINE.transaction, TXN)
            .where(where)
            .fetchOne();
        long gross = nz(roll.get(2, Long.class));
        long tax = nz(roll.get(3, Long.class));
        return new SalesByProductSummary(
            nz(roll.get(0, Long.class)), qtyOf(roll.get(1, BigDecimal.class)), gross, tax, gross,
            roll.get(4, String.class) == null ? "USD" : roll.get(4, String.class));
    }

    /** RPT-AU-TXN — whole POS transactions (header + lines) for a window, newest-first. */
    public PageResponse<TransactionRow> transactions(String locationId, String registerId,
                                                      LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        validateRange(fromDate, toDate);
        BooleanExpression where = txnConds(locationId, registerId, fromDate, toDate);

        long total = query.select(TXN.id.count()).from(TXN).where(where).fetchOne();

        List<PosTransaction> txns = query.selectFrom(TXN)
            .where(where)
            .orderBy(TXN.occurredAt.desc(), TXN.id.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Map<Long, List<TxnLineRow>> linesByTxn = fetchLines(txns);

        List<TransactionRow> content = txns.stream()
            .map(t -> new TransactionRow(
                t.getPublicId(), t.getReceiptNumber(), t.getType().name(), t.getStatus().name(),
                t.getLocationId(), t.getRegisterId(), t.getCashierId(), t.getCustomerId(),
                linesByTxn.getOrDefault(t.getId(), List.of()),
                t.getSubtotalAmount(), t.getTaxAmount(), t.getTotalAmount(), t.getCurrency(),
                t.getOccurredAt()))
            .toList();

        return page(content, pageable, total);
    }

    /** Summary block for the transactions register — count/currency/truncated flag. */
    public TransactionsSummary transactionsSummary(String locationId, String registerId,
                                                    LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        validateRange(fromDate, toDate);
        BooleanExpression where = txnConds(locationId, registerId, fromDate, toDate);
        long total = query.select(TXN.id.count()).from(TXN).where(where).fetchOne();
        String currency = query.select(TXN.currency).from(TXN).where(where).limit(1).fetchFirst();
        long returned = Math.min(pageable.getPageSize(), Math.max(0, total - pageable.getOffset()));
        boolean truncated = total > (pageable.getOffset() + returned);
        return new TransactionsSummary(total, currency == null ? "USD" : currency, truncated);
    }

    private Map<Long, List<TxnLineRow>> fetchLines(List<PosTransaction> txns) {
        if (txns.isEmpty()) {
            return Map.of();
        }
        List<Long> txnIds = txns.stream().map(PosTransaction::getId).toList();
        List<Tuple> lineRows = query.select(
                LINE.transaction.id, LINE.publicId, LINE.productId, LINE.sku, LINE.name,
                LINE.qty, LINE.unitPriceAmount, LINE.discountAmount, LINE.taxAmount,
                LINE.lineNetAmount, LINE.currency)
            .from(LINE)
            .where(LINE.transaction.id.in(txnIds))
            .orderBy(LINE.transaction.id.asc(), LINE.lineNo.asc())
            .fetch();

        Map<Long, List<TxnLineRow>> byTxn = new LinkedHashMap<>();
        for (Tuple t : lineRows) {
            Long txnId = t.get(0, Long.class);
            byTxn.computeIfAbsent(txnId, k -> new ArrayList<>()).add(new TxnLineRow(
                t.get(1, String.class), t.get(2, String.class), t.get(3, String.class),
                t.get(4, String.class), qtyOf(t.get(5, BigDecimal.class)),
                nz(t.get(6, Long.class)), nz(t.get(7, Long.class)), nz(t.get(8, Long.class)),
                nz(t.get(9, Long.class)), t.get(10, String.class)));
        }
        return byTxn;
    }

    private BooleanExpression saleConds(String locationId, LocalDate fromDate, LocalDate toDate) {
        BooleanExpression where = TXN.type.eq(PosTransactionType.SALE)
            .and(TXN.status.eq(PosTransactionStatus.COMPLETED));
        if (locationId != null) {
            where = where.and(TXN.locationId.eq(locationId));
        }
        if (fromDate != null) {
            where = where.and(TXN.occurredAt.goe(startOfDayUtc(fromDate)));
        }
        if (toDate != null) {
            where = where.and(TXN.occurredAt.lt(startOfDayUtc(toDate.plusDays(1))));
        }
        return where;
    }

    private BooleanExpression txnConds(String locationId, String registerId, LocalDate fromDate, LocalDate toDate) {
        BooleanExpression where = TXN.status.in(PosTransactionStatus.COMPLETED, PosTransactionStatus.VOIDED)
            .and(TXN.type.in(PosTransactionType.SALE, PosTransactionType.REFUND));
        if (locationId != null) {
            where = where.and(TXN.locationId.eq(locationId));
        }
        if (registerId != null) {
            where = where.and(TXN.registerId.eq(registerId));
        }
        if (fromDate != null) {
            where = where.and(TXN.occurredAt.goe(startOfDayUtc(fromDate)));
        }
        if (toDate != null) {
            where = where.and(TXN.occurredAt.lt(startOfDayUtc(toDate.plusDays(1))));
        }
        return where;
    }

    private static Instant startOfDayUtc(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static <T> PageResponse<T> page(List<T> content, Pageable pageable, long total) {
        int totalPages = pageable.getPageSize() == 0 ? 0 : (int) Math.ceil((double) total / pageable.getPageSize());
        return new PageResponse<>(content, pageable.getPageNumber(), pageable.getPageSize(), total, totalPages);
    }

    private static void validateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && toDate.isBefore(fromDate)) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "toDate must be on or after fromDate");
        }
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }

    private static double qtyOf(BigDecimal v) {
        return v == null ? 0d : v.doubleValue();
    }
}

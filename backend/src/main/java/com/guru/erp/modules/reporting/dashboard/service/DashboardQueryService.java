package com.guru.erp.modules.reporting.dashboard.service;

import com.guru.erp.modules.finance.gl.domain.JournalEntryStatus;
import com.guru.erp.modules.finance.gl.domain.QJournalEntry;
import com.guru.erp.modules.pos.transactions.domain.PosTransactionStatus;
import com.guru.erp.modules.pos.transactions.domain.PosTransactionType;
import com.guru.erp.modules.pos.transactions.domain.QPosTransaction;
import com.guru.erp.modules.pos.transactions.domain.QPosTransactionLine;
import com.guru.erp.modules.product.catalog.domain.QProduct;
import com.guru.erp.modules.reporting.dashboard.dto.DashboardDtos.DashboardSummaryResponse;
import com.guru.erp.modules.reporting.dashboard.dto.DashboardDtos.PeriodBlock;
import com.guru.erp.modules.reporting.dashboard.dto.DashboardDtos.SeriesPoint;
import com.guru.erp.modules.reporting.dashboard.dto.DashboardDtos.TopProductRow;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executive dashboard summary (reference {@code app.reports.repositories.dashboard.build_dashboard}),
 * scoped to what this codebase has actually ported so far: {@code pos.transactions} (revenue/txn
 * KPIs + top products), {@code finance.gl} (unposted journal count), and {@code product.catalog}
 * (product count, product display names). The reference's procurement/CRM/audit/low-stock
 * roll-ups are intentionally NOT reproduced here — those modules are not yet ported in this
 * codebase, and fabricating numbers for them would mislead the KPI tiles rather than help them.
 *
 * <p>Every period block is one QueryDSL aggregation query (grouped by SQL date-of-occurred-at) —
 * no per-day Java loop re-querying the database. All money is minor-unit {@code long}.
 */
@Service
@Transactional(readOnly = true)
public class DashboardQueryService {

    private static final QPosTransaction TXN = QPosTransaction.posTransaction;
    private static final QPosTransactionLine LINE = QPosTransactionLine.posTransactionLine;
    private static final QProduct PRODUCT = QProduct.product;
    private static final QJournalEntry ENTRY = QJournalEntry.journalEntry;

    private static final int TOP_PRODUCTS_LIMIT = 5;

    private final JPAQueryFactory query;

    public DashboardQueryService(JPAQueryFactory query) {
        this.query = query;
    }

    public DashboardSummaryResponse summary(String locationId) {
        Instant now = Instant.now();
        LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();

        PeriodBlock todayBlock = periodBlock(locationId, today, today, today.minusDays(1), today.minusDays(1));
        PeriodBlock weekBlock = periodBlock(locationId, today.minusDays(6), today, today.minusDays(13), today.minusDays(7));
        PeriodBlock monthBlock = periodBlock(locationId, today.minusDays(29), today, today.minusDays(59), today.minusDays(30));

        List<TopProductRow> topProducts = topProducts(locationId, today.minusDays(29), today);
        long unpostedJournals = unpostedJournalCount();
        long productCount = query.select(PRODUCT.id.count()).from(PRODUCT).fetchOne();
        String currency = monthBlock.currency();

        return new DashboardSummaryResponse(currency, now, todayBlock, weekBlock, monthBlock,
            topProducts, unpostedJournals, productCount);
    }

    private PeriodBlock periodBlock(String locationId, LocalDate from, LocalDate to,
                                    LocalDate prevFrom, LocalDate prevTo) {
        List<SeriesPoint> series = dailySeries(locationId, from, to);
        long revenue = series.stream().mapToLong(SeriesPoint::valueMinor).sum();
        int txnCount = countTxns(locationId, from, to);

        long prevRevenue = dailySeries(locationId, prevFrom, prevTo).stream().mapToLong(SeriesPoint::valueMinor).sum();
        int prevTxnCount = countTxns(locationId, prevFrom, prevTo);

        long avgBasket = txnCount > 0
            ? BigDecimal.valueOf(revenue).divide(BigDecimal.valueOf(txnCount), 0, RoundingMode.HALF_EVEN).longValueExact()
            : 0L;

        TopProductRow topOfPeriod = topProducts(locationId, from, to).stream().findFirst().orElse(null);
        String topName = topOfPeriod == null ? null : topOfPeriod.name();
        long topRevenue = topOfPeriod == null ? 0L : topOfPeriod.revenueMinor();

        String currency = query.select(TXN.currency).from(TXN)
            .where(TXN.type.eq(PosTransactionType.SALE).and(TXN.status.eq(PosTransactionStatus.COMPLETED)))
            .limit(1).fetchFirst();

        return new PeriodBlock(revenue, deltaPct(revenue, prevRevenue), txnCount,
            (int) deltaPct(txnCount, prevTxnCount), avgBasket, topName, topRevenue, series,
            currency == null ? "USD" : currency);
    }

    /** Per-local-calendar-day gross revenue, oldest-first — one grouped SQL query, no Java loop over days. */
    private List<SeriesPoint> dailySeries(String locationId, LocalDate from, LocalDate to) {
        BooleanExpression where = saleConds(locationId, from, to);
        var dayExpr = com.querydsl.core.types.dsl.Expressions.dateTemplate(
            LocalDate.class, "CAST({0} AS date)", TXN.occurredAt);

        List<Tuple> rows = query.select(dayExpr, LINE.lineNetAmount.sum())
            .from(LINE)
            .join(LINE.transaction, TXN)
            .where(where)
            .groupBy(dayExpr)
            .fetch();

        java.util.Map<LocalDate, Long> byDay = new java.util.HashMap<>();
        for (Tuple t : rows) {
            byDay.put(t.get(dayExpr), t.get(1, Long.class));
        }
        List<SeriesPoint> out = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            out.add(new SeriesPoint(d, byDay.getOrDefault(d, 0L)));
        }
        return out;
    }

    private int countTxns(String locationId, LocalDate from, LocalDate to) {
        Long count = query.select(TXN.id.countDistinct())
            .from(TXN)
            .where(saleConds(locationId, from, to))
            .fetchOne();
        return count == null ? 0 : count.intValue();
    }

    /** Top-N products by gross revenue in the window, joined to Product for display name. */
    private List<TopProductRow> topProducts(String locationId, LocalDate from, LocalDate to) {
        BooleanExpression where = saleConds(locationId, from, to);
        List<Tuple> rows = query.select(
                LINE.productId,
                LINE.sku.min(),
                PRODUCT.name.min().coalesce(LINE.name.min()),
                LINE.qty.sum().add(LINE.freeQty.sum()),
                LINE.lineNetAmount.sum(),
                LINE.currency.min())
            .from(LINE)
            .join(LINE.transaction, TXN)
            .leftJoin(PRODUCT).on(PRODUCT.publicId.eq(LINE.productId))
            .where(where)
            .groupBy(LINE.productId)
            .orderBy(LINE.lineNetAmount.sum().desc())
            .limit(TOP_PRODUCTS_LIMIT)
            .fetch();

        return rows.stream()
            .map(t -> new TopProductRow(
                t.get(0, String.class),
                t.get(1, String.class),
                t.get(2, String.class),
                t.get(3, BigDecimal.class) == null ? 0d : t.get(3, BigDecimal.class).doubleValue(),
                t.get(4, Long.class) == null ? 0L : t.get(4, Long.class),
                t.get(5, String.class)))
            .toList();
    }

    private long unpostedJournalCount() {
        Long count = query.select(ENTRY.id.count())
            .from(ENTRY)
            .where(ENTRY.status.eq(JournalEntryStatus.DRAFT))
            .fetchOne();
        return count == null ? 0L : count;
    }

    private BooleanExpression saleConds(String locationId, LocalDate from, LocalDate to) {
        BooleanExpression where = TXN.type.eq(PosTransactionType.SALE)
            .and(TXN.status.eq(PosTransactionStatus.COMPLETED))
            .and(TXN.occurredAt.goe(from.atStartOfDay(ZoneOffset.UTC).toInstant()))
            .and(TXN.occurredAt.lt(to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        if (locationId != null) {
            where = where.and(TXN.locationId.eq(locationId));
        }
        return where;
    }

    private static double deltaPct(long current, long base) {
        if (base <= 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(current - base)
            .divide(BigDecimal.valueOf(base), 4, RoundingMode.HALF_EVEN)
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_EVEN)
            .doubleValue();
    }
}

package com.springboot.erp.modules.inventory.stock.service;

import com.springboot.erp.modules.inventory.stock.domain.MovementType;
import com.springboot.erp.modules.inventory.stock.domain.StockStatus;
import com.springboot.erp.modules.inventory.stock.dto.StockAvailabilityDtos.StockCheckItemRequest;
import com.springboot.erp.modules.inventory.stock.dto.StockAvailabilityDtos.StockCheckItemResponse;
import com.springboot.erp.modules.inventory.stock.dto.StockAvailabilityDtos.StockCheckRequest;
import com.springboot.erp.modules.inventory.stock.dto.StockAvailabilityDtos.StockCheckResponse;
import com.springboot.erp.modules.inventory.stock.dto.StockLedgerDtos.LedgerEntryResponse;
import com.springboot.erp.modules.inventory.stock.dto.StockOnHandDtos.BatchSohRequest;
import com.springboot.erp.modules.inventory.stock.dto.StockOnHandDtos.ProductSohSummary;
import com.springboot.erp.modules.inventory.stock.dto.StockOnHandDtos.StockOnHandResponse;
import com.springboot.erp.modules.inventory.stock.mapper.StockMapper;
import com.springboot.erp.modules.inventory.stock.repository.StockAvailabilityRow;
import com.springboot.erp.modules.inventory.stock.repository.StockLedgerRepository;
import com.springboot.erp.modules.inventory.stock.repository.StockOnHandRow;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-side use-cases for the "stock" slice — pure projections over the
 * append-only ledger (reference {@code stock_on_hand}, {@code stock_availability},
 * {@code ledger}). No mutation, no audit; every method is read-only.
 */
@Service
public class StockQueryService {

    private static final int LEDGER_DEFAULT_WINDOW_DAYS = 90;

    private final StockLedgerRepository ledgerRepository;
    private final StockMapper mapper;

    public StockQueryService(StockLedgerRepository ledgerRepository, StockMapper mapper) {
        this.ledgerRepository = ledgerRepository;
        this.mapper = mapper;
    }

    /** On-hand buckets grouped by (product, variant, location, status) — FR-115. */
    @Transactional(readOnly = true)
    public List<StockOnHandResponse> stockOnHand(String locationId, String productId, String variantId) {
        List<StockOnHandRow> rows = ledgerRepository.aggregateOnHand(locationId, productId, variantId);
        List<StockOnHandResponse> out = new ArrayList<>(rows.size());
        for (StockOnHandRow r : rows) {
            long unitCost = 0L;              // valuation cost resolution is out of this slice's scope
            String currency = "USD";
            long totalValue = value(r.getQtyOnHand(), unitCost);
            out.add(new StockOnHandResponse(r.getProductId(), r.getVariantId(), r.getLocationId(),
                r.getStatus().name(), r.getQtyOnHand(), unitCost, currency, totalValue, r.getUpdatedAt()));
        }
        return out;
    }

    /** Total SOH across all locations for one product (positive buckets only). */
    @Transactional(readOnly = true)
    public ProductSohSummary productSohSummary(String productId, String variantId) {
        List<StockOnHandRow> rows = ledgerRepository.aggregateOnHand(null, productId, variantId);
        BigDecimal total = BigDecimal.ZERO;
        for (StockOnHandRow r : rows) {
            if (r.getQtyOnHand().signum() > 0) {
                total = total.add(r.getQtyOnHand());
            }
        }
        return new ProductSohSummary(productId, variantId, total);
    }

    /** Total SOH for many products in one aggregate query (PO/PR line display). */
    @Transactional(readOnly = true)
    public List<ProductSohSummary> batchSohSummary(BatchSohRequest req) {
        List<String> ids = req.productIds();
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (String id : ids) {
            totals.put(id, BigDecimal.ZERO);
        }
        for (String productId : ids) {
            for (StockOnHandRow r : ledgerRepository.aggregateOnHand(null, productId, null)) {
                if (r.getQtyOnHand().signum() > 0) {
                    totals.merge(productId, r.getQtyOnHand(), BigDecimal::add);
                }
            }
        }
        List<ProductSohSummary> out = new ArrayList<>(ids.size());
        for (String id : ids) {
            out.add(new ProductSohSummary(id, null, totals.getOrDefault(id, BigDecimal.ZERO)));
        }
        return out;
    }

    /** Paginated, filtered, date-windowed ledger listing — FR-117. */
    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> listLedger(String productId, String variantId, String locationId,
                                                MovementType movementType, Instant fromDate, Instant toDate,
                                                int skip, int limit) {
        Instant from = fromDate != null ? fromDate
            : Instant.now().minus(LEDGER_DEFAULT_WINDOW_DAYS, ChronoUnit.DAYS);
        int page = limit > 0 ? skip / limit : 0;
        return ledgerRepository.listEntries(productId, variantId, locationId, movementType, from, toDate,
                PageRequest.of(page, Math.max(limit, 1)))
            .stream().map(mapper::toResponse).toList();
    }

    /** Bulk availability check — one aggregate query for every requested line. */
    @Transactional(readOnly = true)
    public StockCheckResponse checkAvailability(StockCheckRequest req) {
        StockStatus status = req.stockStatus() != null ? req.stockStatus() : StockStatus.AVAILABLE;

        List<String> productIds = req.items().stream().map(StockCheckItemRequest::productId).distinct().toList();
        List<String> locationIds = new ArrayList<>();
        for (StockCheckItemRequest item : req.items()) {
            String loc = item.locationId() != null ? item.locationId() : req.locationId();
            if (loc != null && !locationIds.contains(loc)) {
                locationIds.add(loc);
            }
        }

        // (product, variant|null, location) -> on-hand for the requested status bucket.
        Map<String, BigDecimal> onHandMap = new java.util.HashMap<>();
        if (!productIds.isEmpty() && !locationIds.isEmpty()) {
            for (StockAvailabilityRow r : ledgerRepository.aggregateAvailability(productIds, locationIds, status)) {
                onHandMap.put(key(r.getProductId(), r.getVariantId(), r.getLocationId()), r.getOnHand());
            }
        }

        List<StockCheckItemResponse> out = new ArrayList<>(req.items().size());
        boolean allSufficient = true;
        for (StockCheckItemRequest item : req.items()) {
            String loc = item.locationId() != null ? item.locationId() : req.locationId();
            BigDecimal required = item.qtyRequired() != null ? item.qtyRequired() : BigDecimal.ZERO;
            BigDecimal onHand = BigDecimal.ZERO;
            if (loc != null) {
                if (item.variantId() != null) {
                    onHand = onHandMap.getOrDefault(key(item.productId(), item.variantId(), loc), BigDecimal.ZERO);
                } else {
                    // No variant filter: sum all variant buckets (positive only).
                    for (Map.Entry<String, BigDecimal> e : onHandMap.entrySet()) {
                        if (e.getKey().startsWith(item.productId() + "|") && e.getKey().endsWith("|" + loc)
                            && e.getValue().signum() > 0) {
                            onHand = onHand.add(e.getValue());
                        }
                    }
                }
            }
            boolean sufficient = loc != null && onHand.compareTo(required) >= 0;
            BigDecimal shortfall = required.subtract(onHand).max(BigDecimal.ZERO);
            if (!sufficient) {
                allSufficient = false;
            }
            out.add(new StockCheckItemResponse(item.productId(), item.variantId(), loc,
                onHand, required, sufficient, shortfall));
        }
        return new StockCheckResponse(out, allSufficient);
    }

    private static String key(String productId, String variantId, String locationId) {
        return productId + "|" + (variantId == null ? "" : variantId) + "|" + locationId;
    }

    private static long value(BigDecimal qty, long unitCostMinor) {
        return qty.multiply(BigDecimal.valueOf(unitCostMinor))
            .setScale(0, java.math.RoundingMode.HALF_EVEN).longValueExact();
    }
}

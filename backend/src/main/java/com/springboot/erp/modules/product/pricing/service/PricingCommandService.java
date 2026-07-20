package com.springboot.erp.modules.product.pricing.service;

import com.springboot.erp.modules.product.pricing.domain.PriceField;
import com.springboot.erp.modules.product.pricing.domain.PriceHistory;
import com.springboot.erp.modules.product.pricing.domain.PriceList;
import com.springboot.erp.modules.product.pricing.domain.PriceListItem;
import com.springboot.erp.modules.product.pricing.domain.PriceListStatus;
import com.springboot.erp.modules.product.pricing.domain.ProductLocationOverride;
import com.springboot.erp.modules.product.pricing.dto.PricingDtos.LocationOverrideResponse;
import com.springboot.erp.modules.product.pricing.dto.PricingDtos.LocationOverrideUpsertRequest;
import com.springboot.erp.modules.product.pricing.dto.PricingDtos.PriceHistoryResponse;
import com.springboot.erp.modules.product.pricing.dto.PricingDtos.PriceListCreateRequest;
import com.springboot.erp.modules.product.pricing.dto.PricingDtos.PriceListItemPatchRequest;
import com.springboot.erp.modules.product.pricing.dto.PricingDtos.PriceListItemResponse;
import com.springboot.erp.modules.product.pricing.dto.PricingDtos.PriceListItemUpsertRequest;
import com.springboot.erp.modules.product.pricing.dto.PricingDtos.PriceListResponse;
import com.springboot.erp.modules.product.pricing.dto.PricingDtos.PriceListUpdateRequest;
import com.springboot.erp.modules.product.pricing.dto.PricingDtos.ScheduledPriceChangeRequest;
import com.springboot.erp.modules.product.pricing.mapper.PricingMapper;
import com.springboot.erp.modules.product.pricing.repository.PriceHistoryRepository;
import com.springboot.erp.modules.product.pricing.repository.PriceListItemRepository;
import com.springboot.erp.modules.product.pricing.repository.PriceListRepository;
import com.springboot.erp.modules.product.pricing.repository.ProductLocationOverrideRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.money.Money;
import com.springboot.erp.platform.security.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side use-cases for the pricing slice (US-013). Ports every business rule
 * from the reference views: case-insensitive per-company name uniqueness, the
 * in-use delete guard, non-negative price checks, the FR-061 one-current-price
 * upsert vs FR-062 scheduled-key upsert, append-only price history, and the
 * partial-unique location-override upsert. Every mutation records an audit row
 * in-transaction. Cross-slice product/variant/location/company refs are ULID
 * publicId strings (no DB FK). Kept under the 250-line cap.
 */
@Service
@Transactional
public class PricingCommandService {

    private static final String E_PRICE_LIST = "price_list";
    private static final String E_PRICE_LIST_ITEM = "price_list_item";
    private static final String E_PRICE_HISTORY = "price_history";
    private static final String E_LOCATION_OVERRIDE = "product_location_override";

    private final PriceListRepository priceLists;
    private final PriceListItemRepository priceListItems;
    private final PriceHistoryRepository priceHistory;
    private final ProductLocationOverrideRepository overrides;
    private final PricingMapper mapper;
    private final AuditService auditService;
    private final CurrentUser currentUser;
    private final Clock clock = Clock.systemUTC();

    public PricingCommandService(PriceListRepository priceLists,
                                 PriceListItemRepository priceListItems,
                                 PriceHistoryRepository priceHistory,
                                 ProductLocationOverrideRepository overrides,
                                 PricingMapper mapper, AuditService auditService,
                                 CurrentUser currentUser) {
        this.priceLists = priceLists;
        this.priceListItems = priceListItems;
        this.priceHistory = priceHistory;
        this.overrides = overrides;
        this.mapper = mapper;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    // --- PriceList ---

    public PriceListResponse createPriceList(PriceListCreateRequest req) {
        String name = req.name().strip();
        if (priceLists.existsByCompanyIdAndNameIgnoreCase(req.companyId(), name, -1L)) {
            throw duplicateName(name);
        }
        PriceList pl = new PriceList(req.companyId(), name, req.currency().toUpperCase(),
            req.priceDisplayMode());
        PriceList saved = priceLists.save(pl);
        auditService.record(E_PRICE_LIST, saved.getPublicId(), AuditAction.CREATE, null, snapshot(saved));
        return mapper.toResponse(saved);
    }

    public PriceListResponse updatePriceList(String publicId, PriceListUpdateRequest req) {
        PriceList pl = loadPriceList(publicId);
        assertVersion(pl.getVersion(), req.version());
        Map<String, Object> before = snapshot(pl);
        if (req.name() != null && !req.name().strip().isEmpty()
                && !req.name().strip().equals(pl.getName())) {
            String name = req.name().strip();
            if (priceLists.existsByCompanyIdAndNameIgnoreCase(pl.getCompanyId(), name, pl.getId())) {
                throw duplicateName(name);
            }
            pl.setName(name);
        }
        if (req.status() != null) {
            pl.setStatusEnum(PriceListStatus.fromValue(req.status()));
        }
        PriceList saved = priceLists.save(pl);
        auditService.record(E_PRICE_LIST, saved.getPublicId(), AuditAction.UPDATE, before, snapshot(saved));
        return mapper.toResponse(saved);
    }

    public void deletePriceList(String publicId) {
        PriceList pl = loadPriceList(publicId);
        Map<String, Object> before = snapshot(pl);
        pl.softDelete();
        pl.setStatusEnum(PriceListStatus.INACTIVE);
        priceLists.save(pl);
        auditService.record(E_PRICE_LIST, pl.getPublicId(), AuditAction.DELETE, before, null);
    }

    // --- PriceListItem ---

    public PriceListItemResponse upsertItem(String priceListPublicId, PriceListItemUpsertRequest req) {
        PriceList pl = loadPriceList(priceListPublicId);
        Money price = Money.ofMinor(req.priceAmount(), req.priceCurrency().toUpperCase());
        boolean scheduled = req.effectiveFrom() != null;
        Instant effFrom = scheduled ? req.effectiveFrom() : Instant.now(clock);
        Instant effTo = req.effectiveTo();
        if (effTo != null && !effTo.isAfter(effFrom)) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "effectiveTo must be after effectiveFrom");
        }
        PriceListItem existing = scheduled
            ? priceListItems.findScheduled(pl.getId(), req.productId(), req.variantId(), effFrom).orElse(null)
            : latest(priceListItems.findLatestForProduct(pl.getId(), req.productId(), req.variantId(),
                PageRequest.of(0, 1)).getContent());

        PriceListItem item;
        AuditAction action;
        Map<String, Object> before;
        if (existing != null) {
            before = snapshot(existing);
            existing.setPrice(price);
            existing.setEffectiveFrom(effFrom);
            existing.setEffectiveTo(effTo);
            item = existing;
            action = AuditAction.UPDATE;
        } else {
            before = null;
            item = new PriceListItem(pl, req.productId(), req.variantId(), price, effFrom, effTo);
            action = AuditAction.CREATE;
        }
        PriceListItem saved = priceListItems.save(item);
        auditService.record(E_PRICE_LIST_ITEM, saved.getPublicId(), action, before, snapshot(saved));
        return mapper.toResponse(saved);
    }

    public PriceListItemResponse patchItem(String publicId, PriceListItemPatchRequest req) {
        PriceListItem item = loadItem(publicId);
        Map<String, Object> before = snapshot(item);
        long amount = req.priceAmount() != null ? req.priceAmount() : item.getPrice().amountMinor();
        String currency = req.priceCurrency() != null
            ? req.priceCurrency().toUpperCase() : item.getPrice().currency();
        if (req.priceAmount() != null || req.priceCurrency() != null) {
            item.setPrice(Money.ofMinor(amount, currency));
        }
        if (req.effectiveFrom() != null) {
            item.setEffectiveFrom(req.effectiveFrom());
        }
        if (req.effectiveTo() != null) {
            item.setEffectiveTo(req.effectiveTo());
        }
        if (item.getEffectiveTo() != null && !item.getEffectiveTo().isAfter(item.getEffectiveFrom())) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "effectiveTo must be after effectiveFrom");
        }
        PriceListItem saved = priceListItems.save(item);
        auditService.record(E_PRICE_LIST_ITEM, saved.getPublicId(), AuditAction.UPDATE, before, snapshot(saved));
        return mapper.toResponse(saved);
    }

    public void deleteItem(String publicId) {
        PriceListItem item = loadItem(publicId);
        Map<String, Object> before = snapshot(item);
        priceListItems.delete(item);
        auditService.record(E_PRICE_LIST_ITEM, publicId, AuditAction.DELETE, before, null);
    }

    // --- PriceHistory (FR-062 / FR-064 append-only) ---

    public PriceHistoryResponse scheduleBasePriceChange(ScheduledPriceChangeRequest req) {
        Instant now = Instant.now(clock);
        PriceHistory row = new PriceHistory(req.productId(), req.variantId(), PriceField.SELL,
            req.oldAmount(), req.newSellAmount(), req.currency().toUpperCase(),
            req.effectiveFrom() != null ? req.effectiveFrom() : now, now,
            currentUser.optional().map(p -> p.userPublicId()).orElse(null));
        PriceHistory saved = priceHistory.save(row);
        auditService.record(E_PRICE_HISTORY, saved.getPublicId(), AuditAction.CREATE, null, snapshot(saved));
        return mapper.toResponse(saved);
    }

    // --- ProductLocationOverride (FR-063) ---

    public LocationOverrideResponse upsertOverride(LocationOverrideUpsertRequest req) {
        Money price = Money.ofMinor(req.priceAmount(), req.priceCurrency().toUpperCase());
        ProductLocationOverride existing = overrides
            .findExisting(req.productId(), req.locationId(), req.variantId()).orElse(null);
        ProductLocationOverride row;
        AuditAction action;
        Map<String, Object> before;
        if (existing != null) {
            before = snapshot(existing);
            existing.setPrice(price);
            row = existing;
            action = AuditAction.UPDATE;
        } else {
            before = null;
            row = new ProductLocationOverride(req.productId(), req.locationId(), req.variantId(), price);
            action = AuditAction.CREATE;
        }
        ProductLocationOverride saved = overrides.save(row);
        auditService.record(E_LOCATION_OVERRIDE, saved.getPublicId(), action, before, snapshot(saved));
        return mapper.toResponse(saved);
    }

    public void deleteOverride(String publicId) {
        ProductLocationOverride o = overrides.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("ProductLocationOverride", publicId));
        Map<String, Object> before = snapshot(o);
        o.softDelete();
        overrides.save(o);
        auditService.record(E_LOCATION_OVERRIDE, o.getPublicId(), AuditAction.DELETE, before, null);
    }

    // --- helpers ---

    private PriceList loadPriceList(String publicId) {
        return priceLists.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("PriceList", publicId));
    }

    private PriceListItem loadItem(String publicId) {
        return priceListItems.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("PriceListItem", publicId));
    }

    private static PriceListItem latest(List<PriceListItem> rows) {
        return rows.isEmpty() ? null : rows.get(0);
    }

    private void assertVersion(long actual, Long expected) {
        if (expected != null && expected != actual) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK,
                "PriceList was modified concurrently",
                Map.of("expected", expected, "actual", actual));
        }
    }

    private DomainException duplicateName(String name) {
        return new DomainException(ErrorCode.DUPLICATE,
            "A price list named '%s' already exists for this company".formatted(name),
            Map.of("name", name));
    }

    private Map<String, Object> snapshot(PriceList pl) {
        return Map.of("id", pl.getPublicId(), "companyId", pl.getCompanyId(), "name", pl.getName(),
            "currency", pl.getCurrency(), "status", pl.getStatus());
    }

    private Map<String, Object> snapshot(PriceListItem i) {
        return Map.of("id", i.getPublicId(), "priceListId", i.getPriceList().getPublicId(),
            "productId", i.getProductId(), "priceAmount", i.getPrice().amountMinor(),
            "currency", i.getPrice().currency(), "effectiveFrom", i.getEffectiveFrom().toString());
    }

    private Map<String, Object> snapshot(PriceHistory h) {
        return Map.of("id", h.getPublicId(), "productId", h.getProductId(), "field", h.getField(),
            "newAmount", h.getNewAmount(), "currency", h.getCurrency(),
            "effectiveFrom", h.getEffectiveFrom().toString());
    }

    private Map<String, Object> snapshot(ProductLocationOverride o) {
        return Map.of("id", o.getPublicId(), "productId", o.getProductId(),
            "locationId", o.getLocationId(), "priceAmount", o.getPrice().amountMinor(),
            "currency", o.getPrice().currency());
    }
}

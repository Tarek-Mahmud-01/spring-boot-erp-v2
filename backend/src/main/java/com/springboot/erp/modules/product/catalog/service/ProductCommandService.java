package com.springboot.erp.modules.product.catalog.service;

import com.springboot.erp.modules.product.catalog.domain.LifecycleState;
import com.springboot.erp.modules.product.catalog.domain.Product;
import com.springboot.erp.modules.product.catalog.dto.ProductDtos.ProductCreateRequest;
import com.springboot.erp.modules.product.catalog.dto.ProductDtos.ProductResponse;
import com.springboot.erp.modules.product.catalog.dto.ProductDtos.ProductUpdateRequest;
import com.springboot.erp.modules.product.catalog.mapper.ProductMapper;
import com.springboot.erp.modules.product.catalog.repository.ProductRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.money.Money;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side use-cases for ENT-011 Product: create/update/delete + FR-070
 * lifecycle. Ports SKU format + case-insensitive uniqueness, immutable SKU,
 * non-negative prices, weighed-goods PLU requirement/uniqueness (FR-283), the
 * lifecycle state machine, and soft-delete. Cross-slice refs (category/uom/tax
 * code) are trusted ULIDs. Every mutation records an audit row in-transaction.
 */
@Service
public class ProductCommandService {

    private static final String ENTITY = "product";
    // Alphanumeric start, then alnum/._- up to 50 chars total (reference SKU_PATTERN).
    private static final Pattern SKU_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,49}$");

    private final ProductRepository products;
    private final ProductMapper mapper;
    private final AuditService auditService;

    public ProductCommandService(ProductRepository products, ProductMapper mapper,
                                 AuditService auditService) {
        this.products = products;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional
    public ProductResponse create(ProductCreateRequest req) {
        if (req.sellAmount() < 0 || req.costAmount() < 0) {
            throw priceNegative();
        }
        String sku = validateSku(req.sku());
        if (products.existsBySkuIgnoreCase(sku)) {
            throw new DomainException(ErrorCode.DUPLICATE, "SKU '%s' already exists".formatted(sku),
                Map.of("sku", sku));
        }
        boolean soldByWeight = Boolean.TRUE.equals(req.soldByWeight());
        String plu = assertPluValid(soldByWeight, req.plu(), null);

        Product p = new Product();
        p.setSku(sku);
        p.setName(req.name().strip());
        p.setDescription(req.description());
        p.setCategoryId(req.categoryId());
        p.setUomId(req.uomId());
        p.setBrand(req.brand());
        p.setSupplierId(req.supplierId());
        p.setTaxCodeId(req.taxCodeId());
        p.setCost(Money.ofMinor(req.costAmount(), req.costCurrency()));
        // Initial split: assume the seed cost is pure purchase price (reference).
        p.setPoCostAmount(req.costAmount());
        p.setLandedCostAmount(0);
        p.setSell(Money.ofMinor(req.sellAmount(), req.sellCurrency()));
        p.setWeightGrams(req.weightGrams());
        p.setDimensions(req.dimensions());
        p.setHasVariants(Boolean.TRUE.equals(req.hasVariants()));
        p.setActive(true);
        p.setLifecycleState(LifecycleState.DRAFT);
        p.setRestrictionAge18(Boolean.TRUE.equals(req.restrictionAge18()));
        p.setRestrictionAge21(Boolean.TRUE.equals(req.restrictionAge21()));
        p.setRestrictionControlledDisplay(Boolean.TRUE.equals(req.restrictionControlledDisplay()));
        p.setRestrictionNote(req.restrictionNote());
        p.setSoldByWeight(soldByWeight);
        p.setPlu(plu);
        p.setPricePerKgAmount(req.pricePerKgAmount());

        Product saved = products.save(p);
        auditService.record(ENTITY, saved.getPublicId(), AuditAction.CREATE, null, snapshot(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public ProductResponse update(String publicId, ProductUpdateRequest req) {
        Product p = load(publicId);
        assertVersion(p, req.version());
        if (req.costAmount() != null && req.costAmount() < 0) {
            throw priceNegative();
        }
        if (req.sellAmount() != null && req.sellAmount() < 0) {
            throw priceNegative();
        }
        Map<String, Object> before = snapshot(p);

        if (req.name() != null) {
            p.setName(req.name().strip());
        }
        if (req.description() != null) {
            p.setDescription(req.description());
        }
        if (req.brand() != null) {
            p.setBrand(req.brand());
        }
        if (req.supplierId() != null) {
            p.setSupplierId(req.supplierId());
        }
        if (req.categoryId() != null) {
            p.setCategoryId(req.categoryId());
        }
        if (req.uomId() != null) {
            p.setUomId(req.uomId());
        }
        if (req.taxCodeId() != null) {
            p.setTaxCodeId(req.taxCodeId());
        }
        // Money: change amount and/or currency, preserving the other side.
        if (req.costCurrency() != null || req.costAmount() != null) {
            long amt = req.costAmount() != null ? req.costAmount() : p.getCost().amountMinor();
            String ccy = req.costCurrency() != null ? req.costCurrency() : p.getCost().currency();
            p.setCost(Money.ofMinor(amt, ccy));
        }
        if (req.sellCurrency() != null || req.sellAmount() != null) {
            long amt = req.sellAmount() != null ? req.sellAmount() : p.getSell().amountMinor();
            String ccy = req.sellCurrency() != null ? req.sellCurrency() : p.getSell().currency();
            p.setSell(Money.ofMinor(amt, ccy));
        }
        if (req.weightGrams() != null) {
            p.setWeightGrams(req.weightGrams());
        }
        if (req.dimensions() != null) {
            p.setDimensions(req.dimensions());
        }
        if (req.restrictionAge18() != null) {
            p.setRestrictionAge18(req.restrictionAge18());
        }
        if (req.restrictionAge21() != null) {
            p.setRestrictionAge21(req.restrictionAge21());
        }
        if (req.restrictionControlledDisplay() != null) {
            p.setRestrictionControlledDisplay(req.restrictionControlledDisplay());
        }
        if (req.restrictionNote() != null) {
            p.setRestrictionNote(req.restrictionNote());
        }
        // FR-283 — validate weighed-goods against the RESULTING state.
        if (req.soldByWeight() != null || req.plu() != null) {
            boolean nextSold = req.soldByWeight() != null ? req.soldByWeight() : p.isSoldByWeight();
            String nextPlu = req.plu() != null ? req.plu() : p.getPlu();
            p.setPlu(assertPluValid(nextSold, nextPlu, p.getId()));
            p.setSoldByWeight(nextSold);
        }
        if (req.pricePerKgAmount() != null) {
            p.setPricePerKgAmount(req.pricePerKgAmount());
        }

        Product saved = products.save(p);
        auditService.record(ENTITY, saved.getPublicId(), AuditAction.UPDATE, before, snapshot(saved));
        return mapper.toResponse(saved);
    }

    /** FR-048 / AC-010-5 — soft-delete; also flips is_active off. */
    @Transactional
    public void delete(String publicId) {
        Product p = load(publicId);
        Map<String, Object> before = snapshot(p);
        p.setActive(false);
        p.softDelete();
        products.save(p);
        auditService.record(ENTITY, p.getPublicId(), AuditAction.DELETE, before, null);
    }

    // --- helpers ---

    private String assertPluValid(boolean soldByWeight, String plu, Long excludeId) {
        if (!soldByWeight) {
            return null;
        }
        String cleaned = plu == null ? "" : plu.strip();
        if (cleaned.isEmpty()) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "A weighed product requires a PLU", Map.of("field", "plu"));
        }
        boolean clash = excludeId == null
            ? products.existsByPlu(cleaned)
            : products.existsByPluAndIdNot(cleaned, excludeId);
        if (clash) {
            throw new DomainException(ErrorCode.DUPLICATE,
                "PLU '%s' already in use".formatted(cleaned), Map.of("plu", cleaned));
        }
        return cleaned;
    }

    private static String validateSku(String sku) {
        String cleaned = sku == null ? "" : sku.strip();
        if (!SKU_PATTERN.matcher(cleaned).matches()) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "SKU has an invalid format", Map.of("reason", "sku_format"));
        }
        return cleaned;
    }

    private Product load(String publicId) {
        return products.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("Product", publicId));
    }

    private void assertVersion(Product p, Long expected) {
        if (expected != null && expected != p.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK, "Product was modified concurrently",
                Map.of("expected", expected, "actual", p.getVersion()));
        }
    }

    private static DomainException priceNegative() {
        return new DomainException(ErrorCode.VALIDATION_FAILED, "Price must not be negative",
            Map.of("field", "price"));
    }

    private Map<String, Object> snapshot(Product p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getPublicId());
        m.put("sku", p.getSku());
        m.put("name", p.getName());
        m.put("categoryId", p.getCategoryId());
        m.put("uomId", p.getUomId());
        m.put("taxCodeId", p.getTaxCodeId());
        m.put("costAmount", p.getCost().amountMinor());
        m.put("sellAmount", p.getSell().amountMinor());
        m.put("sellCurrency", p.getSell().currency());
        m.put("hasVariants", p.isHasVariants());
        m.put("isActive", p.isActive());
        m.put("lifecycleState", p.getLifecycleState().name());
        m.put("soldByWeight", p.isSoldByWeight());
        m.put("plu", p.getPlu());
        m.put("version", p.getVersion());
        return m;
    }
}

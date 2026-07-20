package com.springboot.erp.modules.product.catalog.service;

import com.springboot.erp.modules.product.catalog.domain.Product;
import com.springboot.erp.modules.product.catalog.domain.ProductVariant;
import com.springboot.erp.modules.product.catalog.dto.VariantDtos.VariantInputRequest;
import com.springboot.erp.modules.product.catalog.dto.VariantDtos.VariantResponse;
import com.springboot.erp.modules.product.catalog.dto.VariantDtos.VariantUpdateRequest;
import com.springboot.erp.modules.product.catalog.dto.VariantDtos.VariantsCreateRequest;
import com.springboot.erp.modules.product.catalog.mapper.ProductMapper;
import com.springboot.erp.modules.product.catalog.repository.ProductBarcodeRepository;
import com.springboot.erp.modules.product.catalog.repository.ProductRepository;
import com.springboot.erp.modules.product.catalog.repository.ProductVariantRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.money.Money;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ENT-011a ProductVariant use-cases (FR-047 / AC-010-4). SKU is unique per tenant
 * across BOTH products and variants (case-insensitive). Adding the first variant
 * flips the parent's {@code hasVariants}; deleting the last live variant clears
 * it. Deletion is blocked while a barcode still references the variant. Every
 * mutation records an audit row.
 */
@Service
public class VariantService {

    private static final String ENTITY = "product_variant";
    private static final Pattern SKU_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,49}$");

    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final ProductBarcodeRepository barcodes;
    private final ProductMapper mapper;
    private final AuditService auditService;

    public VariantService(ProductRepository products, ProductVariantRepository variants,
                          ProductBarcodeRepository barcodes, ProductMapper mapper,
                          AuditService auditService) {
        this.products = products;
        this.variants = variants;
        this.barcodes = barcodes;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<VariantResponse> list(String productPublicId) {
        Product parent = loadProduct(productPublicId);
        return variants.findByParentProductIdOrderById(parent.getId()).stream()
            .map(v -> mapper.toResponse(v, parent.getPublicId()))
            .toList();
    }

    @Transactional
    public List<VariantResponse> create(String productPublicId, VariantsCreateRequest req) {
        Product parent = loadProduct(productPublicId);
        List<VariantResponse> created = new ArrayList<>();
        for (VariantInputRequest in : req.variants()) {
            if (in.sellAmount() < 0 || in.costAmount() < 0) {
                throw priceNegative();
            }
            String sku = validateSku(in.sku());
            assertSkuFree(sku, null);

            ProductVariant v = new ProductVariant();
            v.setParentProductId(parent.getId());
            v.setSku(sku);
            v.setAttributes(in.attributes() == null ? Map.of() : in.attributes());
            v.setCost(Money.ofMinor(in.costAmount(), in.costCurrency()));
            v.setPoCostAmount(in.costAmount());
            v.setLandedCostAmount(0);
            v.setSell(Money.ofMinor(in.sellAmount(), in.sellCurrency()));
            v.setTaxCodeId(in.taxCodeId());
            ProductVariant saved = variants.save(v);
            auditService.record(ENTITY, saved.getPublicId(), AuditAction.CREATE, null,
                snapshot(saved, parent.getPublicId()));
            created.add(mapper.toResponse(saved, parent.getPublicId()));
        }
        if (!parent.isHasVariants()) {
            parent.setHasVariants(true);
            products.save(parent);
        }
        return created;
    }

    @Transactional
    public VariantResponse update(String productPublicId, String variantPublicId,
                                  VariantUpdateRequest req) {
        Product parent = loadProduct(productPublicId);
        ProductVariant v = loadVariant(parent, variantPublicId);
        assertVersion(v, req.version());
        if (req.costAmount() != null && req.costAmount() < 0) {
            throw priceNegative();
        }
        if (req.sellAmount() != null && req.sellAmount() < 0) {
            throw priceNegative();
        }
        Map<String, Object> before = snapshot(v, parent.getPublicId());

        if (req.sku() != null) {
            String newSku = validateSku(req.sku());
            if (!newSku.equalsIgnoreCase(v.getSku())) {
                assertSkuFree(newSku, v.getId());
                v.setSku(newSku);
            }
        }
        if (req.attributes() != null) {
            v.setAttributes(req.attributes());
        }
        if (req.costCurrency() != null || req.costAmount() != null) {
            long amt = req.costAmount() != null ? req.costAmount() : v.getCost().amountMinor();
            String ccy = req.costCurrency() != null ? req.costCurrency() : v.getCost().currency();
            v.setCost(Money.ofMinor(amt, ccy));
        }
        if (req.sellCurrency() != null || req.sellAmount() != null) {
            long amt = req.sellAmount() != null ? req.sellAmount() : v.getSell().amountMinor();
            String ccy = req.sellCurrency() != null ? req.sellCurrency() : v.getSell().currency();
            v.setSell(Money.ofMinor(amt, ccy));
        }
        if (Boolean.TRUE.equals(req.taxCodeSet())) {
            v.setTaxCodeId(req.taxCodeId());
        }
        ProductVariant saved = variants.save(v);
        auditService.record(ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            snapshot(saved, parent.getPublicId()));
        return mapper.toResponse(saved, parent.getPublicId());
    }

    @Transactional
    public void delete(String productPublicId, String variantPublicId) {
        Product parent = loadProduct(productPublicId);
        ProductVariant v = loadVariant(parent, variantPublicId);
        if (barcodes.existsByVariantId(v.getId())) {
            throw new DomainException(ErrorCode.REFERENCED,
                "Variant is referenced by a barcode and cannot be deleted",
                Map.of("variantId", v.getPublicId()));
        }
        Map<String, Object> before = snapshot(v, parent.getPublicId());
        v.softDelete();
        variants.save(v);
        if (variants.countByParentProductIdAndIdNot(parent.getId(), v.getId()) == 0) {
            parent.setHasVariants(false);
            products.save(parent);
        }
        auditService.record(ENTITY, v.getPublicId(), AuditAction.DELETE, before, null);
    }

    // --- helpers ---

    private void assertSkuFree(String sku, Long excludeVariantId) {
        boolean productClash = products.existsBySkuIgnoreCase(sku);
        boolean variantClash = excludeVariantId == null
            ? variants.existsBySkuIgnoreCase(sku)
            : variants.existsBySkuIgnoreCaseAndIdNot(sku, excludeVariantId);
        if (productClash || variantClash) {
            throw new DomainException(ErrorCode.DUPLICATE, "SKU '%s' already exists".formatted(sku),
                Map.of("sku", sku));
        }
    }

    private static String validateSku(String sku) {
        String cleaned = sku == null ? "" : sku.strip();
        if (!SKU_PATTERN.matcher(cleaned).matches()) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "SKU has an invalid format",
                Map.of("reason", "sku_format"));
        }
        return cleaned;
    }

    private Product loadProduct(String publicId) {
        return products.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("Product", publicId));
    }

    private ProductVariant loadVariant(Product parent, String variantPublicId) {
        ProductVariant v = variants.findByPublicId(variantPublicId)
            .orElseThrow(() -> DomainException.notFound("ProductVariant", variantPublicId));
        if (!v.getParentProductId().equals(parent.getId())) {
            throw DomainException.notFound("ProductVariant", variantPublicId);
        }
        return v;
    }

    private void assertVersion(ProductVariant v, Long expected) {
        if (expected != null && expected != v.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK, "Variant was modified concurrently",
                Map.of("expected", expected, "actual", v.getVersion()));
        }
    }

    private static DomainException priceNegative() {
        return new DomainException(ErrorCode.VALIDATION_FAILED, "Price must not be negative",
            Map.of("field", "price"));
    }

    private Map<String, Object> snapshot(ProductVariant v, String parentPublicId) {
        return Map.of(
            "id", v.getPublicId(),
            "parentProductId", parentPublicId,
            "sku", v.getSku(),
            "attributes", v.getAttributes() == null ? Map.of() : v.getAttributes(),
            "costAmount", v.getCost().amountMinor(),
            "sellAmount", v.getSell().amountMinor(),
            "sellCurrency", v.getSell().currency(),
            "version", v.getVersion());
    }
}

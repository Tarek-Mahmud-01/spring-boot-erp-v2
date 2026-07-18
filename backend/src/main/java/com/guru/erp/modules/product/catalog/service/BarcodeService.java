package com.guru.erp.modules.product.catalog.service;

import com.guru.erp.modules.product.catalog.domain.BarcodeFormat;
import com.guru.erp.modules.product.catalog.domain.BarcodeValidator;
import com.guru.erp.modules.product.catalog.domain.Product;
import com.guru.erp.modules.product.catalog.domain.ProductBarcode;
import com.guru.erp.modules.product.catalog.domain.ProductVariant;
import com.guru.erp.modules.product.catalog.dto.BarcodeDtos.BarcodeCreateRequest;
import com.guru.erp.modules.product.catalog.dto.BarcodeDtos.BarcodeResponse;
import com.guru.erp.modules.product.catalog.mapper.ProductMapper;
import com.guru.erp.modules.product.catalog.repository.ProductBarcodeRepository;
import com.guru.erp.modules.product.catalog.repository.ProductRepository;
import com.guru.erp.modules.product.catalog.repository.ProductVariantRepository;
import com.guru.erp.platform.audit.AuditAction;
import com.guru.erp.platform.audit.AuditService;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ENT-011b ProductBarcode use-cases (FR-056–059). Format validation (EAN-13 /
 * UPC-A checksums, CODE128 charset) is delegated to {@link BarcodeValidator}.
 * At most one primary per product (AC-012-3 — adding/promoting a primary clears
 * the previous one). Globally-unique barcode value; primary barcodes cannot be
 * deleted (promote another first). Every mutation records an audit row.
 */
@Service
public class BarcodeService {

    private static final String ENTITY = "product_barcode";

    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final ProductBarcodeRepository barcodes;
    private final ProductMapper mapper;
    private final AuditService auditService;

    public BarcodeService(ProductRepository products, ProductVariantRepository variants,
                          ProductBarcodeRepository barcodes, ProductMapper mapper,
                          AuditService auditService) {
        this.products = products;
        this.variants = variants;
        this.barcodes = barcodes;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<BarcodeResponse> list(String productPublicId) {
        Product p = loadProduct(productPublicId);
        return barcodes.findByProductIdOrderById(p.getId()).stream()
            .map(b -> mapper.toResponse(b, p.getPublicId(), variantPublicId(b.getVariantId())))
            .toList();
    }

    @Transactional
    public BarcodeResponse create(String productPublicId, BarcodeCreateRequest req) {
        Product p = loadProduct(productPublicId);
        BarcodeFormat fmt = req.format();
        String cleaned = BarcodeValidator.validate(req.barcode(), fmt);
        if (cleaned == null) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Barcode is invalid for format " + fmt.name(), Map.of("format", fmt.name()));
        }
        Long variantInternalId = null;
        if (req.variantId() != null) {
            ProductVariant v = variants.findByPublicId(req.variantId())
                .orElseThrow(() -> DomainException.notFound("ProductVariant", req.variantId()));
            if (!v.getParentProductId().equals(p.getId())) {
                throw DomainException.notFound("ProductVariant", req.variantId());
            }
            variantInternalId = v.getId();
        }
        if (barcodes.existsByBarcode(cleaned)) {
            throw new DomainException(ErrorCode.DUPLICATE,
                "Barcode '%s' already exists".formatted(cleaned), Map.of("barcode", cleaned));
        }
        boolean isPrimary = Boolean.TRUE.equals(req.isPrimary());
        if (isPrimary) {
            clearPrimary(p.getId(), null);
        }
        ProductBarcode row = new ProductBarcode();
        row.setProductId(p.getId());
        row.setVariantId(variantInternalId);
        row.setBarcode(cleaned);
        row.setPrimary(isPrimary);
        row.setFormat(fmt);
        ProductBarcode saved = barcodes.save(row);
        auditService.record(ENTITY, saved.getPublicId(), AuditAction.CREATE, null, snapshot(saved));
        return mapper.toResponse(saved, p.getPublicId(), variantPublicId(saved.getVariantId()));
    }

    /** AC-012-3 — promote a barcode to primary, clearing the previous primary. */
    @Transactional
    public BarcodeResponse setPrimary(String barcodePublicId) {
        ProductBarcode row = barcodes.findByPublicId(barcodePublicId)
            .orElseThrow(() -> DomainException.notFound("ProductBarcode", barcodePublicId));
        Map<String, Object> before = snapshot(row);
        clearPrimary(row.getProductId(), row.getId());
        row.setPrimary(true);
        ProductBarcode saved = barcodes.save(row);
        auditService.record(ENTITY, saved.getPublicId(), AuditAction.UPDATE, before, snapshot(saved));
        Product p = products.findById(saved.getProductId()).orElseThrow();
        return mapper.toResponse(saved, p.getPublicId(), variantPublicId(saved.getVariantId()));
    }

    @Transactional
    public void delete(String barcodePublicId) {
        ProductBarcode row = barcodes.findByPublicId(barcodePublicId)
            .orElseThrow(() -> DomainException.notFound("ProductBarcode", barcodePublicId));
        if (row.isPrimary()) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Primary barcode cannot be deleted; promote another first",
                Map.of("barcodeId", row.getPublicId()));
        }
        Map<String, Object> before = snapshot(row);
        barcodes.delete(row);
        auditService.record(ENTITY, barcodePublicId, AuditAction.DELETE, before, null);
    }

    // --- helpers ---

    private void clearPrimary(Long productId, Long exceptId) {
        for (ProductBarcode existing : barcodes.findByProductIdAndIsPrimaryTrue(productId)) {
            if (exceptId == null || !existing.getId().equals(exceptId)) {
                existing.setPrimary(false);
                barcodes.save(existing);
            }
        }
    }

    private String variantPublicId(Long variantInternalId) {
        if (variantInternalId == null) {
            return null;
        }
        return variants.findById(variantInternalId).map(ProductVariant::getPublicId).orElse(null);
    }

    private Product loadProduct(String publicId) {
        return products.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("Product", publicId));
    }

    private Map<String, Object> snapshot(ProductBarcode b) {
        return Map.of(
            "id", b.getPublicId(),
            "productId", b.getProductId(),
            "barcode", b.getBarcode(),
            "format", b.getFormat().name(),
            "isPrimary", b.isPrimary());
    }
}

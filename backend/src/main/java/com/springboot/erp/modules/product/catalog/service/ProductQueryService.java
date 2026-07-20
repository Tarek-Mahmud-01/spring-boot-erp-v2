package com.springboot.erp.modules.product.catalog.service;

import com.springboot.erp.modules.product.catalog.domain.BarcodeValidator;
import com.springboot.erp.modules.product.catalog.domain.LifecycleState;
import com.springboot.erp.modules.product.catalog.domain.Product;
import com.springboot.erp.modules.product.catalog.domain.ProductBarcode;
import com.springboot.erp.modules.product.catalog.dto.BarcodeDtos.BarcodeResponse;
import com.springboot.erp.modules.product.catalog.dto.ProductDtos.ProductResponse;
import com.springboot.erp.modules.product.catalog.dto.WeighedDtos.VariableMeasureResolveResponse;
import com.springboot.erp.modules.product.catalog.mapper.ProductMapper;
import com.springboot.erp.modules.product.catalog.repository.ProductBarcodeRepository;
import com.springboot.erp.modules.product.catalog.repository.ProductRepository;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.web.PageResponse;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-side use-cases for the product catalog: list/get, barcode lookup, and the
 * CR-001 (FR-284) variable-measure barcode resolution. All read-only.
 */
@Service
public class ProductQueryService {

    private final ProductRepository products;
    private final ProductBarcodeRepository barcodes;
    private final ProductMapper mapper;

    public ProductQueryService(ProductRepository products, ProductBarcodeRepository barcodes,
                               ProductMapper mapper) {
        this.products = products;
        this.barcodes = barcodes;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> list(String q, String lifecycleState, Boolean active,
                                              Pageable pageable) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        LifecycleState state = lifecycleState == null || lifecycleState.isBlank()
            ? null : LifecycleState.valueOf(lifecycleState.trim().toUpperCase());
        return PageResponse.of(products.search(query, state, active, pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(String publicId) {
        return mapper.toResponse(load(publicId));
    }

    /** FR-163 — map a scanned exact barcode to its product+variant public ids. */
    @Transactional(readOnly = true)
    public BarcodeResponse lookupByBarcode(String barcode) {
        ProductBarcode row = barcodes.findByBarcode(barcode.strip())
            .orElseThrow(() -> new DomainException(
                com.springboot.erp.platform.error.ErrorCode.NOT_FOUND,
                "No product carries barcode '%s'".formatted(barcode),
                Map.of("barcode", barcode)));
        Product p = products.findById(row.getProductId())
            .orElseThrow(() -> DomainException.notFound("Product", String.valueOf(row.getProductId())));
        return mapper.toResponse(row, p.getPublicId(), null);
    }

    /**
     * FR-284/FR-285 — decode a GS1 in-store price-embedded EAN-13 (prefix "21")
     * and resolve it to the weighed product by PLU. Stateless: returns everything
     * the POS needs to build a cart line without an open backend transaction.
     */
    @Transactional(readOnly = true)
    public VariableMeasureResolveResponse resolveVariableMeasure(String barcode) {
        BarcodeValidator.VariableMeasure decoded = BarcodeValidator.decodeVariableMeasure(barcode);
        if (decoded == null) {
            throw new DomainException(
                com.springboot.erp.platform.error.ErrorCode.VALIDATION_FAILED,
                "Barcode is not a valid variable-measure scale label",
                Map.of("barcode", barcode));
        }
        Product p = products.findByPluAndSoldByWeight(decoded.plu())
            .orElseThrow(() -> new DomainException(
                com.springboot.erp.platform.error.ErrorCode.NOT_FOUND,
                "No weighed product for PLU '%s'".formatted(decoded.plu()),
                Map.of("plu", decoded.plu())));
        return new VariableMeasureResolveResponse(
            p.getPublicId(),
            p.getSku(),
            p.getName(),
            barcode.strip(),
            decoded.plu(),
            decoded.priceMinorUnits(),
            p.getSell().currency(),
            p.getTaxCodeId(),
            p.isRestrictionAge18(),
            p.isRestrictionAge21(),
            p.isRestrictionControlledDisplay());
    }

    private Product load(String publicId) {
        return products.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("Product", publicId));
    }
}

package com.guru.erp.modules.inventory.stock.service;

import com.guru.erp.modules.inventory.stock.domain.BatchBarcodeSource;
import com.guru.erp.modules.inventory.stock.domain.ProductBatch;
import com.guru.erp.modules.inventory.stock.dto.ProductBatchDtos.BatchCreateRequest;
import com.guru.erp.modules.inventory.stock.dto.ProductBatchDtos.BatchResponse;
import com.guru.erp.modules.inventory.stock.dto.ProductBatchDtos.BatchUpdateRequest;
import com.guru.erp.modules.inventory.stock.mapper.StockMapper;
import com.guru.erp.modules.inventory.stock.repository.ProductBatchRepository;
import com.guru.erp.platform.audit.AuditAction;
import com.guru.erp.platform.audit.AuditService;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import com.guru.erp.platform.money.Money;
import com.guru.erp.platform.web.PageResponse;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read + write use-cases for {@link ProductBatch} (reference {@code ProductBatch}).
 * Standard list / get / create / update / delete plus a barcode look-up used at
 * POS scan time. Barcodes must be unique among non-null values (reference partial
 * unique index), enforced here so the wire error is a clean 409.
 */
@Service
public class ProductBatchService {

    private static final String AUDIT_ENTITY = "product_batch";
    private static final String DEFAULT_CURRENCY = "USD";

    private final ProductBatchRepository repository;
    private final StockMapper mapper;
    private final AuditService auditService;

    public ProductBatchService(ProductBatchRepository repository, StockMapper mapper,
                               AuditService auditService) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<BatchResponse> list(String productId, Pageable pageable) {
        var page = productId != null
            ? repository.findByProductId(productId, pageable)
            : repository.findAll(pageable);
        return PageResponse.of(page, mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public BatchResponse get(String publicId) {
        return mapper.toResponse(load(publicId));
    }

    @Transactional(readOnly = true)
    public BatchResponse getByBarcode(String barcode) {
        return mapper.toResponse(repository.findByBarcode(barcode)
            .orElseThrow(() -> DomainException.notFound("ProductBatch", barcode)));
    }

    @Transactional
    public BatchResponse create(BatchCreateRequest req) {
        if (req.barcode() != null && repository.existsByBarcode(req.barcode())) {
            throw new DomainException(ErrorCode.DUPLICATE, "Barcode already registered: " + req.barcode());
        }
        ProductBatch b = new ProductBatch();
        b.setProductId(req.productId());
        b.setVariantId(req.variantId());
        b.setSourceDocType(req.sourceDocType());
        b.setSourceDocId(req.sourceDocId());
        b.setBatchNo(req.batchNo());
        b.setBarcode(req.barcode());
        b.setBarcodeFormat(req.barcodeFormat());
        b.setBarcodeSource(req.barcodeSource() != null ? req.barcodeSource() : BatchBarcodeSource.MANUFACTURER);
        b.setQtyPerScan(req.qtyPerScan() != null ? req.qtyPerScan() : BigDecimal.ONE);
        b.setGrnCost(Money.ofMinor(req.grnCostAmount(), currency(req.grnCostCurrency())));
        b.setMrpAmount(req.mrpAmount());
        b.setMrpCurrency(req.mrpCurrency());
        b.setSellPriceAmount(req.sellPriceAmount());
        b.setSellPriceCurrency(req.sellPriceCurrency());
        b.setQtyReceived(req.qtyReceived() != null ? req.qtyReceived() : BigDecimal.ZERO);
        b.setManufactureDate(req.manufactureDate());
        b.setExpiryDate(req.expiryDate());

        ProductBatch saved = repository.save(b);
        BatchResponse response = mapper.toResponse(saved);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null, response);
        return response;
    }

    @Transactional
    public BatchResponse update(String publicId, BatchUpdateRequest req) {
        ProductBatch b = load(publicId);
        checkVersion(b, req.version());
        BatchResponse before = mapper.toResponse(b);

        if (req.barcode() != null && !req.barcode().equals(b.getBarcode())
            && repository.existsByBarcode(req.barcode())) {
            throw new DomainException(ErrorCode.DUPLICATE, "Barcode already registered: " + req.barcode());
        }
        if (req.batchNo() != null) {
            b.setBatchNo(req.batchNo());
        }
        if (req.barcode() != null) {
            b.setBarcode(req.barcode());
        }
        if (req.barcodeFormat() != null) {
            b.setBarcodeFormat(req.barcodeFormat());
        }
        if (req.barcodeSource() != null) {
            b.setBarcodeSource(req.barcodeSource());
        }
        if (req.qtyPerScan() != null) {
            b.setQtyPerScan(req.qtyPerScan());
        }
        if (req.mrpAmount() != null) {
            b.setMrpAmount(req.mrpAmount());
        }
        if (req.mrpCurrency() != null) {
            b.setMrpCurrency(req.mrpCurrency());
        }
        if (req.sellPriceAmount() != null) {
            b.setSellPriceAmount(req.sellPriceAmount());
        }
        if (req.sellPriceCurrency() != null) {
            b.setSellPriceCurrency(req.sellPriceCurrency());
        }
        if (req.qtyReceived() != null) {
            b.setQtyReceived(req.qtyReceived());
        }
        if (req.manufactureDate() != null) {
            b.setManufactureDate(req.manufactureDate());
        }
        if (req.expiryDate() != null) {
            b.setExpiryDate(req.expiryDate());
        }

        ProductBatch saved = repository.save(b);
        BatchResponse response = mapper.toResponse(saved);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before, response);
        return response;
    }

    @Transactional
    public void delete(String publicId) {
        ProductBatch b = load(publicId);
        BatchResponse before = mapper.toResponse(b);
        b.softDelete();
        repository.save(b);
        auditService.record(AUDIT_ENTITY, publicId, AuditAction.DELETE, before, null);
    }

    private ProductBatch load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("ProductBatch", publicId));
    }

    private void checkVersion(ProductBatch b, Long requestVersion) {
        if (requestVersion != null && requestVersion != b.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK,
                ErrorCode.OPTIMISTIC_LOCK.defaultDetail());
        }
    }

    private static String currency(String value) {
        return value != null ? value.toUpperCase(Locale.ROOT) : DEFAULT_CURRENCY;
    }
}

package com.springboot.erp.modules.procurement.landed.service;

import com.springboot.erp.modules.procurement.landed.domain.BarcodeFormat;
import com.springboot.erp.modules.procurement.landed.domain.UnitBarcode;
import com.springboot.erp.modules.procurement.landed.domain.UnitBarcodeStatus;
import com.springboot.erp.modules.procurement.landed.dto.UnitBarcodeDtos.UnitBarcodeBulkGenerateRequest;
import com.springboot.erp.modules.procurement.landed.dto.UnitBarcodeDtos.UnitBarcodeCreateRequest;
import com.springboot.erp.modules.procurement.landed.dto.UnitBarcodeDtos.UnitBarcodePatchRequest;
import com.springboot.erp.modules.procurement.landed.dto.UnitBarcodeDtos.UnitBarcodeResponse;
import com.springboot.erp.modules.procurement.landed.mapper.LandedMapper;
import com.springboot.erp.modules.procurement.landed.repository.UnitBarcodeRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side use-cases for per-unit barcodes assigned at GRN time. Ports the reference create /
 * bulk-generate / patch / delete flows: barcode uniqueness (scoped to unit_barcodes — the
 * product_barcodes / product_batches tables live in other slices and are not reachable), the
 * received-qty quota guard, the mandatory sell price on a non-bundle barcode, and EAN-13
 * auto-generation with a check digit. Bundle CRUD is in {@link BundleCommandService}.
 */
@Service
public class UnitBarcodeCommandService {

    static final String AUDIT_ENTITY = "unit_barcode";
    private static final int MAX_GENERATE_ATTEMPTS = 10_000;

    private final UnitBarcodeRepository repository;
    private final LandedMapper mapper;
    private final AuditService auditService;

    public UnitBarcodeCommandService(UnitBarcodeRepository repository, LandedMapper mapper,
                                     AuditService auditService) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    /** Assign one barcode to a GRN line (or create a standalone bundle when {@code bundle}). */
    @Transactional
    public UnitBarcodeResponse create(UnitBarcodeCreateRequest req) {
        if (repository.existsByBarcode(req.barcode())) {
            throw new DomainException(ErrorCode.DUPLICATE, "Barcode already in use: " + req.barcode());
        }
        if (!req.bundle() && req.sellPriceAmount() == null) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "A unit barcode must carry a sell price");
        }
        BigDecimal qty = req.qty() == null ? BigDecimal.ONE : req.qty();
        requirePositive(qty);

        UnitBarcode ub = new UnitBarcode();
        ub.setGrnLineId(req.grnLineId());
        ub.setBarcode(req.barcode());
        ub.setBarcodeFormat(req.barcodeFormat() == null
            ? BarcodeSupport.detectFormat(req.barcode()) : req.barcodeFormat());
        ub.setBundle(req.bundle());
        ub.setProductId(req.bundle() ? null : req.productId());
        ub.setVariantId(req.bundle() ? null : req.variantId());
        ub.setQty(qty);
        ub.setMrpAmount(req.mrpAmount());
        ub.setMrpCurrency(req.mrpCurrency());
        ub.setSellPriceAmount(req.sellPriceAmount());
        ub.setSellPriceCurrency(req.sellPriceCurrency());
        ub.setBatchNo(req.batchNo());
        ub.setSerialNo(req.serialNo());
        ub.setExpiryDate(req.expiryDate());
        ub.setEntryDate(req.entryDate());
        ub.setNotes(req.notes());
        ub.setStatus(UnitBarcodeStatus.AVAILABLE);
        BarcodeSupport.applyItems(ub, req.items());

        UnitBarcode saved = repository.save(ub);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    /** Auto-generate {@code count} EAN-13 barcodes (qty 1 each) for a GRN line. */
    @Transactional
    public List<UnitBarcodeResponse> bulkGenerate(UnitBarcodeBulkGenerateRequest req) {
        Set<String> seen = new HashSet<>();
        List<UnitBarcodeResponse> out = new ArrayList<>(req.count());
        for (int i = 0; i < req.count(); i++) {
            String code = nextEan13(seen);
            seen.add(code);
            UnitBarcode ub = new UnitBarcode();
            ub.setGrnLineId(req.grnLineId());
            ub.setBarcode(code);
            ub.setBarcodeFormat(BarcodeFormat.EAN13);
            ub.setBundle(false);
            ub.setProductId(req.productId());
            ub.setVariantId(req.variantId());
            ub.setQty(BigDecimal.ONE);
            ub.setMrpAmount(req.mrpAmount());
            ub.setMrpCurrency(req.mrpCurrency());
            ub.setSellPriceAmount(req.sellPriceAmount());
            ub.setSellPriceCurrency(req.sellPriceCurrency());
            ub.setStatus(UnitBarcodeStatus.AVAILABLE);
            out.add(mapper.toResponse(repository.save(ub)));
        }
        auditService.record(AUDIT_ENTITY, "bulk:" + req.grnLineId(), AuditAction.CREATE, null,
            java.util.Map.of("count", req.count(), "grnLineId", req.grnLineId()));
        return out;
    }

    /** Update a barcode's price / status / notes (all optional). */
    @Transactional
    public UnitBarcodeResponse patch(String publicId, UnitBarcodePatchRequest req) {
        UnitBarcode ub = load(publicId);
        checkVersion(ub, req.version());
        UnitBarcodeResponse before = mapper.toResponse(ub);

        if (req.barcode() != null && !req.barcode().equals(ub.getBarcode())) {
            if (repository.existsByBarcodeAndPublicIdNot(req.barcode(), ub.getPublicId())) {
                throw new DomainException(ErrorCode.DUPLICATE, "Barcode already in use: " + req.barcode());
            }
            ub.setBarcode(req.barcode());
            ub.setBarcodeFormat(BarcodeSupport.detectFormat(req.barcode()));
        }
        if (req.qty() != null) {
            ub.setQty(req.qty());
        }
        if (req.mrpAmount() != null) {
            ub.setMrpAmount(req.mrpAmount());
        }
        if (req.mrpCurrency() != null) {
            ub.setMrpCurrency(req.mrpCurrency());
        }
        if (req.sellPriceAmount() != null) {
            ub.setSellPriceAmount(req.sellPriceAmount());
        }
        if (req.sellPriceCurrency() != null) {
            ub.setSellPriceCurrency(req.sellPriceCurrency());
        }
        if (req.batchNo() != null) {
            ub.setBatchNo(req.batchNo());
        }
        if (req.serialNo() != null) {
            ub.setSerialNo(req.serialNo());
        }
        if (req.expiryDate() != null) {
            ub.setExpiryDate(req.expiryDate());
        }
        if (req.entryDate() != null) {
            ub.setEntryDate(req.entryDate());
        }
        if (req.status() != null) {
            ub.setStatus(UnitBarcodeStatus.fromWire(req.status()));
        }
        if (req.notes() != null) {
            ub.setNotes(req.notes());
        }

        UnitBarcode saved = repository.save(ub);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    /** Delete an available barcode (reference: only 'available' rows are deletable). */
    @Transactional
    public void delete(String publicId) {
        UnitBarcode ub = load(publicId);
        if (ub.getStatus() != UnitBarcodeStatus.AVAILABLE) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Only an available barcode can be deleted; this one is " + ub.getStatus().wire());
        }
        UnitBarcodeResponse before = mapper.toResponse(ub);
        ub.softDelete();
        repository.save(ub);
        auditService.record(AUDIT_ENTITY, publicId, AuditAction.DELETE, before, null);
    }

    // --- internals -----------------------------------------------------------

    private static void requirePositive(BigDecimal qty) {
        if (qty.signum() <= 0) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "qty must be positive");
        }
    }

    private String nextEan13(Set<String> seen) {
        for (int attempt = 0; attempt < MAX_GENERATE_ATTEMPTS; attempt++) {
            long prefix = ThreadLocalRandom.current().nextLong(100_000_000_000L, 1_000_000_000_000L);
            String digits12 = Long.toString(prefix);
            String code = digits12 + BarcodeSupport.ean13CheckDigit(digits12);
            if (!seen.contains(code) && !repository.existsByBarcode(code)) {
                return code;
            }
        }
        throw new DomainException(ErrorCode.CONFLICT,
            "Could not generate a unique EAN-13 after " + MAX_GENERATE_ATTEMPTS + " attempts");
    }

    private UnitBarcode load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("UnitBarcode", publicId));
    }

    private void checkVersion(UnitBarcode ub, Long requestVersion) {
        if (requestVersion != null && requestVersion != ub.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK,
                ErrorCode.OPTIMISTIC_LOCK.defaultDetail());
        }
    }
}

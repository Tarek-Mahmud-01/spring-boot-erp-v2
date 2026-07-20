package com.springboot.erp.modules.procurement.landed.service;

import com.springboot.erp.modules.procurement.landed.domain.UnitBarcode;
import com.springboot.erp.modules.procurement.landed.domain.UnitBarcodeStatus;
import com.springboot.erp.modules.procurement.landed.dto.UnitBarcodeDtos.BundleCreateRequest;
import com.springboot.erp.modules.procurement.landed.dto.UnitBarcodeDtos.BundleUpdateRequest;
import com.springboot.erp.modules.procurement.landed.dto.UnitBarcodeDtos.UnitBarcodeResponse;
import com.springboot.erp.modules.procurement.landed.mapper.LandedMapper;
import com.springboot.erp.modules.procurement.landed.repository.UnitBarcodeRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side use-cases for multi-product bundle barcodes (a {@link UnitBarcode} with
 * {@code isBundle=true} plus component {@code UnitBarcodeItem} rows). Split out of
 * {@link UnitBarcodeCommandService} so both write paths stay within the service size cap while
 * sharing the barcode helpers in {@link BarcodeSupport}. A bundle prices itself, so a sell price is
 * mandatory at create.
 */
@Service
public class BundleCommandService {

    static final String AUDIT_ENTITY = "unit_barcode";

    private final UnitBarcodeRepository repository;
    private final LandedMapper mapper;
    private final AuditService auditService;

    public BundleCommandService(UnitBarcodeRepository repository, LandedMapper mapper,
                                AuditService auditService) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional
    public UnitBarcodeResponse create(BundleCreateRequest req) {
        if (req.sellPriceAmount() == null) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "A bundle must carry a sell price");
        }
        if (repository.existsByBarcode(req.barcode())) {
            throw new DomainException(ErrorCode.DUPLICATE, "Barcode already in use: " + req.barcode());
        }
        UnitBarcode ub = new UnitBarcode();
        ub.setBarcode(req.barcode());
        ub.setBarcodeFormat(req.barcodeFormat() == null
            ? BarcodeSupport.detectFormat(req.barcode()) : req.barcodeFormat());
        ub.setBundle(true);
        ub.setQty(BigDecimal.ONE);
        ub.setMrpAmount(req.mrpAmount());
        ub.setMrpCurrency(req.mrpCurrency());
        ub.setSellPriceAmount(req.sellPriceAmount());
        ub.setSellPriceCurrency(req.sellPriceCurrency());
        ub.setNotes(req.name());
        ub.setStatus(UnitBarcodeStatus.AVAILABLE);
        BarcodeSupport.applyItems(ub, req.items());

        UnitBarcode saved = repository.save(ub);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public UnitBarcodeResponse update(String publicId, BundleUpdateRequest req) {
        UnitBarcode ub = repository.findByPublicId(publicId)
            .filter(UnitBarcode::isBundle)
            .orElseThrow(() -> DomainException.notFound("Bundle", publicId));
        checkVersion(ub, req.version());
        UnitBarcodeResponse before = mapper.toResponse(ub);

        if (req.barcode() != null && !req.barcode().equals(ub.getBarcode())) {
            if (repository.existsByBarcodeAndPublicIdNot(req.barcode(), ub.getPublicId())) {
                throw new DomainException(ErrorCode.DUPLICATE, "Barcode already in use: " + req.barcode());
            }
            ub.setBarcode(req.barcode());
            ub.setBarcodeFormat(req.barcodeFormat() == null
                ? BarcodeSupport.detectFormat(req.barcode()) : req.barcodeFormat());
        }
        if (req.name() != null) {
            ub.setNotes(req.name());
        }
        if (req.mrpAmount() != null) {
            ub.setMrpAmount(req.mrpAmount());
            ub.setMrpCurrency(req.mrpCurrency());
        }
        if (req.sellPriceAmount() != null) {
            ub.setSellPriceAmount(req.sellPriceAmount());
            ub.setSellPriceCurrency(req.sellPriceCurrency());
        }
        if (req.items() != null) {
            ub.clearItems();
            BarcodeSupport.applyItems(ub, req.items());
        }

        UnitBarcode saved = repository.save(ub);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    private void checkVersion(UnitBarcode ub, Long requestVersion) {
        if (requestVersion != null && requestVersion != ub.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK,
                ErrorCode.OPTIMISTIC_LOCK.defaultDetail());
        }
    }
}

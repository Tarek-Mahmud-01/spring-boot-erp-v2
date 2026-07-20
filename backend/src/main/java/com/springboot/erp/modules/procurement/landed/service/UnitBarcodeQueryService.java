package com.springboot.erp.modules.procurement.landed.service;

import com.springboot.erp.modules.procurement.landed.domain.UnitBarcodeStatus;
import com.springboot.erp.modules.procurement.landed.dto.UnitBarcodeDtos.BarcodeFreeCheckResponse;
import com.springboot.erp.modules.procurement.landed.dto.UnitBarcodeDtos.GrnLineSummaryResponse;
import com.springboot.erp.modules.procurement.landed.dto.UnitBarcodeDtos.LabelSizeResponse;
import com.springboot.erp.modules.procurement.landed.dto.UnitBarcodeDtos.UnitBarcodeResponse;
import com.springboot.erp.modules.procurement.landed.mapper.LandedMapper;
import com.springboot.erp.modules.procurement.landed.repository.LabelSizeRepository;
import com.springboot.erp.modules.procurement.landed.repository.UnitBarcodeRepository;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.web.PageResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-side use-cases for unit barcodes, bundles, label sizes, and the duplicate-barcode check. */
@Service
public class UnitBarcodeQueryService {

    private final UnitBarcodeRepository repository;
    private final LabelSizeRepository labelSizes;
    private final LandedMapper mapper;

    public UnitBarcodeQueryService(UnitBarcodeRepository repository, LabelSizeRepository labelSizes,
                                   LandedMapper mapper) {
        this.repository = repository;
        this.labelSizes = labelSizes;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<UnitBarcodeResponse> list(String grnLineId, String grnId, String productId,
                                                  String status, String search, Pageable pageable) {
        UnitBarcodeStatus st = status == null || status.isBlank()
            ? null : UnitBarcodeStatus.fromWire(status.trim());
        return PageResponse.of(
            repository.search(blankToNull(grnLineId), blankToNull(grnId), blankToNull(productId),
                Boolean.FALSE, st, blankToNull(search), pageable),
            mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UnitBarcodeResponse get(String publicId) {
        return mapper.toResponse(repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("UnitBarcode", publicId)));
    }

    @Transactional(readOnly = true)
    public PageResponse<UnitBarcodeResponse> listBundles(String search, Pageable pageable) {
        return PageResponse.of(
            repository.search(null, null, null, Boolean.TRUE, null, blankToNull(search), pageable),
            mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UnitBarcodeResponse getBundle(String publicId) {
        var ub = repository.findByPublicId(publicId)
            .filter(com.springboot.erp.modules.procurement.landed.domain.UnitBarcode::isBundle)
            .orElseThrow(() -> DomainException.notFound("Bundle", publicId));
        return mapper.toResponse(ub);
    }

    /** Duplicate-barcode check (scoped to the unit_barcodes table in this slice). */
    @Transactional(readOnly = true)
    public BarcodeFreeCheckResponse check(String barcode) {
        return new BarcodeFreeCheckResponse(repository.existsByBarcode(barcode));
    }

    /** Quota summary for a GRN line — the Σ qty already assigned to it. */
    @Transactional(readOnly = true)
    public GrnLineSummaryResponse summary(String grnLineId) {
        BigDecimal assigned = repository.sumQtyByGrnLineId(grnLineId);
        return new GrnLineSummaryResponse(grnLineId, null,
            assigned == null ? BigDecimal.ZERO : assigned);
    }

    @Transactional(readOnly = true)
    public List<LabelSizeResponse> listLabelSizes() {
        return labelSizes.findByActiveTrueOrderBySortOrderAscIdAsc().stream()
            .map(mapper::toResponse).toList();
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}

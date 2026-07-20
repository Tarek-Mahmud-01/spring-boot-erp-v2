package com.springboot.erp.modules.procurement.landed.controller;

import com.springboot.erp.modules.procurement.landed.dto.UnitBarcodeDtos.BarcodeFreeCheckResponse;
import com.springboot.erp.modules.procurement.landed.dto.UnitBarcodeDtos.BundleCreateRequest;
import com.springboot.erp.modules.procurement.landed.dto.UnitBarcodeDtos.BundleUpdateRequest;
import com.springboot.erp.modules.procurement.landed.dto.UnitBarcodeDtos.GrnLineSummaryResponse;
import com.springboot.erp.modules.procurement.landed.dto.UnitBarcodeDtos.LabelSizeResponse;
import com.springboot.erp.modules.procurement.landed.dto.UnitBarcodeDtos.UnitBarcodeBulkGenerateRequest;
import com.springboot.erp.modules.procurement.landed.dto.UnitBarcodeDtos.UnitBarcodeCreateRequest;
import com.springboot.erp.modules.procurement.landed.dto.UnitBarcodeDtos.UnitBarcodePatchRequest;
import com.springboot.erp.modules.procurement.landed.dto.UnitBarcodeDtos.UnitBarcodeResponse;
import com.springboot.erp.modules.procurement.landed.service.BundleCommandService;
import com.springboot.erp.modules.procurement.landed.service.UnitBarcodeCommandService;
import com.springboot.erp.modules.procurement.landed.service.UnitBarcodeQueryService;
import com.springboot.erp.platform.web.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unit-barcode / bundle / label-size endpoints — thin controller, {@code @PreAuthorize} per method.
 * Business rules (quota, uniqueness, EAN-13 generation, sell-price requirement) live in the command
 * / query services. PDF label rendering is deferred (a print-layer concern).
 */
@RestController
@RequestMapping("/api/procurement")
public class UnitBarcodeController {

    private final UnitBarcodeCommandService command;
    private final BundleCommandService bundleCommand;
    private final UnitBarcodeQueryService query;

    public UnitBarcodeController(UnitBarcodeCommandService command, BundleCommandService bundleCommand,
                                 UnitBarcodeQueryService query) {
        this.command = command;
        this.bundleCommand = bundleCommand;
        this.query = query;
    }

    // --- Unit barcodes ---

    @GetMapping("/unit-barcodes")
    @PreAuthorize("hasAuthority('procurement.unit_barcode.read')")
    public PageResponse<UnitBarcodeResponse> list(
            @RequestParam(required = false) String grnLineId,
            @RequestParam(required = false) String grnId,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        return query.list(grnLineId, grnId, productId, status, search, pageable);
    }

    @GetMapping("/unit-barcodes/check")
    @PreAuthorize("hasAuthority('procurement.unit_barcode.read')")
    public BarcodeFreeCheckResponse check(@RequestParam String barcode) {
        return query.check(barcode);
    }

    @GetMapping("/unit-barcodes/grn-line/{grnLineId}/summary")
    @PreAuthorize("hasAuthority('procurement.unit_barcode.read')")
    public GrnLineSummaryResponse summary(@PathVariable String grnLineId) {
        return query.summary(grnLineId);
    }

    @GetMapping("/unit-barcodes/{publicId}")
    @PreAuthorize("hasAuthority('procurement.unit_barcode.read')")
    public UnitBarcodeResponse get(@PathVariable String publicId) {
        return query.get(publicId);
    }

    @PostMapping("/unit-barcodes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('procurement.unit_barcode.write')")
    public UnitBarcodeResponse create(@Valid @RequestBody UnitBarcodeCreateRequest request) {
        return command.create(request);
    }

    @PostMapping("/unit-barcodes/bulk-generate")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('procurement.unit_barcode.write')")
    public List<UnitBarcodeResponse> bulkGenerate(@Valid @RequestBody UnitBarcodeBulkGenerateRequest request) {
        return command.bulkGenerate(request);
    }

    @PatchMapping("/unit-barcodes/{publicId}")
    @PreAuthorize("hasAuthority('procurement.unit_barcode.write')")
    public UnitBarcodeResponse patch(@PathVariable String publicId,
                                     @Valid @RequestBody UnitBarcodePatchRequest request) {
        return command.patch(publicId, request);
    }

    @DeleteMapping("/unit-barcodes/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('procurement.unit_barcode.write')")
    public void delete(@PathVariable String publicId) {
        command.delete(publicId);
    }

    // --- Bundles ---

    @GetMapping("/bundles")
    @PreAuthorize("hasAuthority('procurement.unit_barcode.read')")
    public PageResponse<UnitBarcodeResponse> listBundles(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        return query.listBundles(search, pageable);
    }

    @GetMapping("/bundles/{publicId}")
    @PreAuthorize("hasAuthority('procurement.unit_barcode.read')")
    public UnitBarcodeResponse getBundle(@PathVariable String publicId) {
        return query.getBundle(publicId);
    }

    @PostMapping("/bundles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('procurement.unit_barcode.write')")
    public UnitBarcodeResponse createBundle(@Valid @RequestBody BundleCreateRequest request) {
        return bundleCommand.create(request);
    }

    @PatchMapping("/bundles/{publicId}")
    @PreAuthorize("hasAuthority('procurement.unit_barcode.write')")
    public UnitBarcodeResponse updateBundle(@PathVariable String publicId,
                                            @Valid @RequestBody BundleUpdateRequest request) {
        return bundleCommand.update(publicId, request);
    }

    // --- Label sizes ---

    @GetMapping("/label-sizes")
    @PreAuthorize("hasAuthority('procurement.label_size.read')")
    public List<LabelSizeResponse> labelSizes() {
        return query.listLabelSizes();
    }
}

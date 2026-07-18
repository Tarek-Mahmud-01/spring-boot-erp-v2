package com.guru.erp.modules.procurement.orders.controller;

import com.guru.erp.modules.procurement.orders.dto.PoDtos.PoResponse;
import com.guru.erp.modules.procurement.orders.dto.PrDtos.PrCreateRequest;
import com.guru.erp.modules.procurement.orders.dto.PrDtos.PrResponse;
import com.guru.erp.modules.procurement.orders.dto.PrDtos.PrTransitionRequest;
import com.guru.erp.modules.procurement.orders.dto.PrDtos.PrUpdateRequest;
import com.guru.erp.modules.procurement.orders.service.PurchaseRequisitionCommandService;
import com.guru.erp.modules.procurement.orders.service.PurchaseRequisitionConversionService;
import com.guru.erp.modules.procurement.orders.service.PurchaseRequisitionQueryService;
import com.guru.erp.platform.web.PageResponse;
import jakarta.validation.Valid;
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
 * ENT-027 PurchaseRequisition endpoints — thin controller, {@code @PreAuthorize} per method.
 * Business rules live in the command/query/conversion services. Special endpoints (submit /
 * transition / convert-to-PO) map the reference PR sub-actions.
 */
@RestController
@RequestMapping("/api/procurement/purchase-requisitions")
public class PurchaseRequisitionController {

    private final PurchaseRequisitionCommandService command;
    private final PurchaseRequisitionQueryService query;
    private final PurchaseRequisitionConversionService conversion;

    public PurchaseRequisitionController(PurchaseRequisitionCommandService command,
                                         PurchaseRequisitionQueryService query,
                                         PurchaseRequisitionConversionService conversion) {
        this.command = command;
        this.query = query;
        this.conversion = conversion;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('procurement.requisition.read')")
    public PageResponse<PrResponse> list(
            @RequestParam(required = false) String locationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        return query.list(locationId, status, search, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('procurement.requisition.read')")
    public PrResponse get(@PathVariable String publicId) {
        return query.get(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('procurement.requisition.write')")
    public PrResponse create(@Valid @RequestBody PrCreateRequest request) {
        return command.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('procurement.requisition.write')")
    public PrResponse update(@PathVariable String publicId,
                             @Valid @RequestBody PrUpdateRequest request) {
        return command.update(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('procurement.requisition.write')")
    public void delete(@PathVariable String publicId) {
        command.delete(publicId);
    }

    /** FR-081 / FR-083 — submit the requisition for approval (Draft → Submitted). */
    @PostMapping("/{publicId}/submit")
    @PreAuthorize("hasAuthority('procurement.requisition.write')")
    public PrResponse submit(@PathVariable String publicId) {
        return command.submit(publicId);
    }

    /** FR-085 / FR-086 — workflow move (under-review / send-to-supplier / reject). */
    @PostMapping("/{publicId}/transition")
    @PreAuthorize("hasAuthority('procurement.requisition.write')")
    public PrResponse transition(@PathVariable String publicId,
                                 @Valid @RequestBody PrTransitionRequest request) {
        return command.transition(publicId, request);
    }

    /** FR-084 — one-click convert an Under Review requisition into a Draft purchase order. */
    @PostMapping("/{publicId}/convert-to-po")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('procurement.order.write')")
    public PoResponse convertToPo(@PathVariable String publicId) {
        return conversion.convertToPo(publicId);
    }
}

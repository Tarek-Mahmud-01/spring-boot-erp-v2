package com.guru.erp.modules.procurement.receipts.controller;

import com.guru.erp.modules.procurement.receipts.dto.ReceiptDtos.GrnCreateRequest;
import com.guru.erp.modules.procurement.receipts.dto.ReceiptDtos.GrnPatchRequest;
import com.guru.erp.modules.procurement.receipts.dto.ReceiptDtos.GrnResponse;
import com.guru.erp.modules.procurement.receipts.dto.ReceiptDtos.GrnTransitionRequest;
import com.guru.erp.modules.procurement.receipts.service.ReceiptCommandService;
import com.guru.erp.modules.procurement.receipts.service.ReceiptQueryService;
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
 * ENT-029 GoodsReceipt endpoints — thin controller, {@code @PreAuthorize} per method. Business
 * rules live in the command/query services. Special endpoints (transition / confirm) map the
 * reference GRN workflow sub-actions; confirm posts stock via the outbox.
 */
@RestController
@RequestMapping("/api/procurement/receipts")
public class ReceiptController {

    private final ReceiptCommandService command;
    private final ReceiptQueryService query;

    public ReceiptController(ReceiptCommandService command, ReceiptQueryService query) {
        this.command = command;
        this.query = query;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('procurement.receipt.read')")
    public PageResponse<GrnResponse> list(
            @RequestParam(required = false) String poId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean autoReceipt,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        return query.list(poId, status, autoReceipt, search, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('procurement.receipt.read')")
    public GrnResponse get(@PathVariable String publicId) {
        return query.get(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('procurement.receipt.write')")
    public GrnResponse create(@Valid @RequestBody GrnCreateRequest request) {
        return command.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('procurement.receipt.write')")
    public GrnResponse update(@PathVariable String publicId,
                              @Valid @RequestBody GrnPatchRequest request) {
        return command.update(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('procurement.receipt.write')")
    public void delete(@PathVariable String publicId) {
        command.delete(publicId);
    }

    /** Non-stock transition: Draft→Approved, Approved→Partially Received. */
    @PostMapping("/{publicId}/transition")
    @PreAuthorize("hasAuthority('procurement.receipt.write')")
    public GrnResponse transition(@PathVariable String publicId,
                                  @Valid @RequestBody GrnTransitionRequest request) {
        return command.transition(publicId, request.toStatus());
    }

    /** FR-097 — confirm ("receive"): → Received, posts stock via the outbox + rolls up to the PO. */
    @PostMapping("/{publicId}/confirm")
    @PreAuthorize("hasAuthority('procurement.receipt.write')")
    public GrnResponse confirm(@PathVariable String publicId) {
        return command.confirm(publicId);
    }
}

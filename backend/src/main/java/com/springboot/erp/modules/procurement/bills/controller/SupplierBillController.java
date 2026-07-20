package com.springboot.erp.modules.procurement.bills.controller;

import com.springboot.erp.modules.procurement.bills.dto.BillDtos.BillCreateRequest;
import com.springboot.erp.modules.procurement.bills.dto.BillDtos.BillResponse;
import com.springboot.erp.modules.procurement.bills.dto.BillDtos.BillUpdateRequest;
import com.springboot.erp.modules.procurement.bills.service.BillCommandService;
import com.springboot.erp.modules.procurement.bills.service.BillQueryService;
import com.springboot.erp.platform.web.PageResponse;
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
 * ENT-030 SupplierBill endpoints — thin controller, {@code @PreAuthorize} per method. Business
 * rules (3-way match, auto-approve, payable posting) live in the command/query services. The
 * {@code /approve} endpoint maps the reference manual-approve sub-action.
 */
@RestController
@RequestMapping("/api/procurement/bills")
public class SupplierBillController {

    private final BillCommandService command;
    private final BillQueryService query;

    public SupplierBillController(BillCommandService command, BillQueryService query) {
        this.command = command;
        this.query = query;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('procurement.bill.read')")
    public PageResponse<BillResponse> list(
            @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) String poId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        return query.list(supplierId, poId, status, search, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('procurement.bill.read')")
    public BillResponse get(@PathVariable String publicId) {
        return query.get(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('procurement.bill.write')")
    public BillResponse create(@Valid @RequestBody BillCreateRequest request) {
        return command.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('procurement.bill.write')")
    public BillResponse update(@PathVariable String publicId,
                               @Valid @RequestBody BillUpdateRequest request) {
        return command.update(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('procurement.bill.write')")
    public void delete(@PathVariable String publicId) {
        command.delete(publicId);
    }

    /** FR-100 — manually approve a bill (books AP via the outbox, or parks it Invoiced Not Received). */
    @PostMapping("/{publicId}/approve")
    @PreAuthorize("hasAuthority('procurement.bill.write')")
    public BillResponse approve(@PathVariable String publicId,
                                @RequestParam(required = false) Long version) {
        return command.approve(publicId, version);
    }
}

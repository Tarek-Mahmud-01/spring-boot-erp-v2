package com.springboot.erp.modules.pos.auxiliary.controller;

import com.springboot.erp.modules.pos.auxiliary.dto.RefundDtos.RefundResponse;
import com.springboot.erp.modules.pos.auxiliary.service.RefundQueryService;
import com.springboot.erp.platform.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * PosRefund read endpoints (US-034 FR-177..180). Refund *creation* is not exposed here as a thin
 * write endpoint: per the vertical-slice rule, issuing a refund requires the (not-yet-ported)
 * PosTransaction aggregate to first build the REFUND transaction + its lines (the reference's
 * {@code refund_receipt_linked} / {@code refund_no_receipt} tail) — that workflow belongs to the
 * transactions sub-slice, which calls {@link com.springboot.erp.modules.pos.auxiliary.service.RefundCommandService}
 * once it exists. This controller exposes the read side (lookup + list) that is fully self-contained
 * within this slice today.
 */
@RestController
@RequestMapping("/api/pos/refunds")
public class RefundController {

    private final RefundQueryService queryService;

    public RefundController(RefundQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('pos.refund.read')")
    public PageResponse<RefundResponse> list(
            @RequestParam(required = false) String originalTransactionId,
            @PageableDefault(size = 50) Pageable pageable) {
        return queryService.list(originalTransactionId, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('pos.refund.read')")
    public RefundResponse get(@PathVariable String publicId) {
        return queryService.get(publicId);
    }

    /** Idempotent lookup by the owning REFUND transaction's public id (offline-replay dedupe). */
    @GetMapping("/by-transaction/{transactionId}")
    @PreAuthorize("hasAuthority('pos.refund.read')")
    public RefundResponse getByTransaction(@PathVariable String transactionId) {
        return queryService.getByTransactionId(transactionId);
    }
}

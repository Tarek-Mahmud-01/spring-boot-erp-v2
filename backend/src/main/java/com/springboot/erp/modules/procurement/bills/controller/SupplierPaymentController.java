package com.springboot.erp.modules.procurement.bills.controller;

import com.springboot.erp.modules.procurement.bills.dto.PaymentDtos.PaymentCreateRequest;
import com.springboot.erp.modules.procurement.bills.dto.PaymentDtos.PaymentResponse;
import com.springboot.erp.modules.procurement.bills.dto.PaymentDtos.PaymentTransitionRequest;
import com.springboot.erp.modules.procurement.bills.dto.PaymentDtos.PaymentUpdateRequest;
import com.springboot.erp.modules.procurement.bills.service.PaymentCommandService;
import com.springboot.erp.modules.procurement.bills.service.PaymentQueryService;
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
 * ENT-033 SupplierPayment endpoints — thin controller, {@code @PreAuthorize} per method. The
 * {@code /transition} endpoint maps the reference status-transition sub-action (Draft → Approved →
 * Partially Paid → Paid), which posts the V-003 voucher via the outbox on approval.
 */
@RestController
@RequestMapping("/api/procurement/supplier-payments")
public class SupplierPaymentController {

    private final PaymentCommandService command;
    private final PaymentQueryService query;

    public SupplierPaymentController(PaymentCommandService command, PaymentQueryService query) {
        this.command = command;
        this.query = query;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('procurement.payment.read')")
    public PageResponse<PaymentResponse> list(
            @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) String poId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        return query.list(supplierId, poId, status, search, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('procurement.payment.read')")
    public PaymentResponse get(@PathVariable String publicId) {
        return query.get(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('procurement.payment.write')")
    public PaymentResponse create(@Valid @RequestBody PaymentCreateRequest request) {
        return command.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('procurement.payment.write')")
    public PaymentResponse update(@PathVariable String publicId,
                                  @Valid @RequestBody PaymentUpdateRequest request) {
        return command.update(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('procurement.payment.write')")
    public void delete(@PathVariable String publicId) {
        command.delete(publicId);
    }

    /** Status transition (approve/pay): posts the V-003 payment voucher via the outbox on approval. */
    @PostMapping("/{publicId}/transition")
    @PreAuthorize("hasAuthority('procurement.payment.write')")
    public PaymentResponse transition(@PathVariable String publicId,
                                      @Valid @RequestBody PaymentTransitionRequest request) {
        return command.transition(publicId, request.toStatus());
    }
}

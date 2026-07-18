package com.guru.erp.modules.procurement.bills.controller;

import com.guru.erp.modules.procurement.bills.dto.AmountReceivedDtos.AmountReceivedCreateRequest;
import com.guru.erp.modules.procurement.bills.dto.AmountReceivedDtos.AmountReceivedResponse;
import com.guru.erp.modules.procurement.bills.dto.AmountReceivedDtos.AmountReceivedTransitionRequest;
import com.guru.erp.modules.procurement.bills.dto.AmountReceivedDtos.AmountReceivedUpdateRequest;
import com.guru.erp.modules.procurement.bills.service.AmountReceivedService;
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
 * ENT-034 AmountReceived endpoints — thin controller, {@code @PreAuthorize} per method. The
 * {@code /transition} endpoint maps the reference status-transition sub-action (Draft → Approved →
 * Confirmed → Voided), which posts / reverses the V-004 receipt voucher via the outbox.
 */
@RestController
@RequestMapping("/api/procurement/amount-received")
public class AmountReceivedController {

    private final AmountReceivedService service;

    public AmountReceivedController(AmountReceivedService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('procurement.amount_received.read')")
    public PageResponse<AmountReceivedResponse> list(
            @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        return service.list(supplierId, status, search, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('procurement.amount_received.read')")
    public AmountReceivedResponse get(@PathVariable String publicId) {
        return service.get(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('procurement.amount_received.write')")
    public AmountReceivedResponse create(@Valid @RequestBody AmountReceivedCreateRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('procurement.amount_received.write')")
    public AmountReceivedResponse update(@PathVariable String publicId,
                                         @Valid @RequestBody AmountReceivedUpdateRequest request) {
        return service.update(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('procurement.amount_received.write')")
    public void delete(@PathVariable String publicId) {
        service.delete(publicId);
    }

    /** Status transition (approve/confirm/void): posts or reverses the V-004 voucher via the outbox. */
    @PostMapping("/{publicId}/transition")
    @PreAuthorize("hasAuthority('procurement.amount_received.write')")
    public AmountReceivedResponse transition(@PathVariable String publicId,
                                             @Valid @RequestBody AmountReceivedTransitionRequest request) {
        return service.transition(publicId, request.toStatus(), request.reason());
    }
}

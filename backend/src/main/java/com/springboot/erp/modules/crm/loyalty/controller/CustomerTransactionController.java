package com.springboot.erp.modules.crm.loyalty.controller;

import com.springboot.erp.modules.crm.loyalty.dto.CustomerTransactionDtos.CustomerTransactionIngestRequest;
import com.springboot.erp.modules.crm.loyalty.dto.CustomerTransactionDtos.CustomerTransactionResponse;
import com.springboot.erp.modules.crm.loyalty.service.CustomerTransactionService;
import com.springboot.erp.platform.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Purchase-history projection endpoints (US-041 / FR-215-217) — thin
 * controller, {@code @PreAuthorize} per method. Business rules live in
 * {@link CustomerTransactionService}. {@code POST .../transactions} is a
 * stand-in ingestion endpoint for the POS/Sales outbox feed (reference
 * {@code ingest_transaction}) until a dedicated outbox subscriber is wired.
 */
@RestController
@RequestMapping("/api/crm/loyalty/customers/{customerId}/transactions")
public class CustomerTransactionController {

    private final CustomerTransactionService service;

    public CustomerTransactionController(CustomerTransactionService service) {
        this.service = service;
    }

    /** FR-215 — most recent transactions first, server-paged. */
    @GetMapping
    @PreAuthorize("hasAuthority('crm.customertransaction.read')")
    public PageResponse<CustomerTransactionResponse> listHistory(@PathVariable String customerId,
                                                                 @PageableDefault(size = 50) Pageable pageable) {
        return PageResponse.of(service.listHistory(customerId, pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('crm.customertransaction.write')")
    public CustomerTransactionResponse ingest(@PathVariable String customerId,
                                              @Valid @RequestBody CustomerTransactionIngestRequest request) {
        return service.ingest(customerId, request);
    }
}

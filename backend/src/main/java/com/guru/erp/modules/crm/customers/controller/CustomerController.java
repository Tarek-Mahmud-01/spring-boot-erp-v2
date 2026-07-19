package com.guru.erp.modules.crm.customers.controller;

import com.guru.erp.modules.crm.customers.dto.CustomerDtos.ConsentLogEntry;
import com.guru.erp.modules.crm.customers.dto.CustomerDtos.ConsentToggleRequest;
import com.guru.erp.modules.crm.customers.dto.CustomerDtos.CustomerCreateRequest;
import com.guru.erp.modules.crm.customers.dto.CustomerDtos.CustomerResponse;
import com.guru.erp.modules.crm.customers.dto.CustomerDtos.CustomerUpdateRequest;
import com.guru.erp.modules.crm.customers.service.CustomerCommandService;
import com.guru.erp.modules.crm.customers.service.CustomerConsentService;
import com.guru.erp.modules.crm.customers.service.CustomerQueryService;
import com.guru.erp.platform.web.PageResponse;
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
 * ENT-050 Customer + ENT-051 CustomerProfile endpoints (US-038 / US-040) —
 * thin controller, {@code @PreAuthorize} per method. Business rules live in
 * the command/query services.
 */
@RestController
@RequestMapping("/api/crm/customers")
public class CustomerController {

    private final CustomerCommandService command;
    private final CustomerConsentService consent;
    private final CustomerQueryService query;

    public CustomerController(CustomerCommandService command, CustomerConsentService consent,
                              CustomerQueryService query) {
        this.command = command;
        this.consent = consent;
        this.query = query;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('crm.customer.read')")
    public PageResponse<CustomerResponse> list(@RequestParam(required = false) String companyId,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String query,
                                               @PageableDefault(size = 50) Pageable pageable) {
        return this.query.list(companyId, status, query, pageable);
    }

    /** FR-210/211/214 — cashier/manager lookup by exact mobile / email / membership id, capped at 10. */
    @GetMapping("/lookup")
    @PreAuthorize("hasAuthority('crm.customer.read')")
    public List<CustomerResponse> lookup(@RequestParam String companyId, @RequestParam String q) {
        return query.lookup(companyId, q);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('crm.customer.read')")
    public CustomerResponse get(@PathVariable String publicId) {
        return query.get(publicId);
    }

    @GetMapping("/{publicId}/consent-log")
    @PreAuthorize("hasAuthority('crm.customer.read')")
    public PageResponse<ConsentLogEntry> consentHistory(@PathVariable String publicId,
                                                        @PageableDefault(size = 50) Pageable pageable) {
        return query.consentHistory(publicId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('crm.customer.write')")
    public CustomerResponse create(@Valid @RequestBody CustomerCreateRequest request) {
        return command.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('crm.customer.write')")
    public CustomerResponse update(@PathVariable String publicId,
                                   @Valid @RequestBody CustomerUpdateRequest request) {
        return command.update(publicId, request);
    }

    /** FR-201 — capture a single-channel consent toggle; actor comes from the authenticated principal. */
    @PostMapping("/{publicId}/consent")
    @PreAuthorize("hasAuthority('crm.customer.consent.write')")
    public CustomerResponse setConsent(@PathVariable String publicId,
                                       @Valid @RequestBody ConsentToggleRequest request) {
        return consent.setConsent(publicId, request);
    }

    /** FR-203 — anonymize in place; history rows are preserved. */
    @PostMapping("/{publicId}/anonymize")
    @PreAuthorize("hasAuthority('crm.customer.write')")
    public CustomerResponse anonymize(@PathVariable String publicId) {
        return consent.anonymize(publicId);
    }

    /** FR-204 — hard delete, rejected when the customer has any history. */
    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('crm.customer.write')")
    public void delete(@PathVariable String publicId) {
        command.delete(publicId);
    }
}

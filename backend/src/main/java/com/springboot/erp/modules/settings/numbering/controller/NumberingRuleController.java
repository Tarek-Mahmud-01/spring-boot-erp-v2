package com.springboot.erp.modules.settings.numbering.controller;

import com.springboot.erp.modules.settings.numbering.domain.DocumentType;
import com.springboot.erp.modules.settings.numbering.dto.NumberingRuleDtos.NumberingAllocateRequest;
import com.springboot.erp.modules.settings.numbering.dto.NumberingRuleDtos.NumberingAllocateResponse;
import com.springboot.erp.modules.settings.numbering.dto.NumberingRuleDtos.NumberingRuleCreateRequest;
import com.springboot.erp.modules.settings.numbering.dto.NumberingRuleDtos.NumberingRuleResponse;
import com.springboot.erp.modules.settings.numbering.dto.NumberingRuleDtos.NumberingRuleUpdateRequest;
import com.springboot.erp.modules.settings.numbering.service.NumberingRuleService;
import com.springboot.erp.platform.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * ENT-006 Numbering Rule endpoints (US-005). Thin controller: authorize, parse,
 * delegate to {@link NumberingRuleService}. There is no DELETE — a rule that has
 * issued numbers must persist for the audit trail.
 */
@RestController
@RequestMapping("/api/numbering-rules")
public class NumberingRuleController {

    private final NumberingRuleService service;

    public NumberingRuleController(NumberingRuleService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('settings.numbering.read')")
    public PageResponse<NumberingRuleResponse> list(
        @RequestParam(required = false) String companyId,
        @RequestParam(required = false) DocumentType documentType,
        @PageableDefault(size = 20) Pageable pageable) {
        return service.list(companyId, documentType, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('settings.numbering.read')")
    public NumberingRuleResponse get(@PathVariable String publicId) {
        return service.get(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('settings.numbering.write')")
    public NumberingRuleResponse create(@Valid @RequestBody NumberingRuleCreateRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('settings.numbering.write')")
    public NumberingRuleResponse update(
        @PathVariable String publicId,
        @Valid @RequestBody NumberingRuleUpdateRequest request) {
        return service.update(publicId, request);
    }

    @PostMapping("/{publicId}/allocate")
    @PreAuthorize("hasAuthority('settings.numbering.write')")
    public NumberingAllocateResponse allocate(
        @PathVariable String publicId,
        @Valid @RequestBody NumberingAllocateRequest request) {
        return service.allocate(publicId, request);
    }
}

package com.guru.erp.modules.crm.loyalty.controller;

import com.guru.erp.modules.crm.loyalty.dto.LoyaltyConfigDtos.LoyaltyProgramResponse;
import com.guru.erp.modules.crm.loyalty.dto.LoyaltyConfigDtos.LoyaltyProgramUpsertRequest;
import com.guru.erp.modules.crm.loyalty.dto.LoyaltyConfigDtos.TierUpsertRequest;
import com.guru.erp.modules.crm.loyalty.service.LoyaltyConfigService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ENT-072 LoyaltyConfig endpoints (US-039 / FR-205-207) — thin controller,
 * {@code @PreAuthorize} per method. Business rules live in {@link LoyaltyConfigService}.
 * One program per company: {@code PUT /companies/{companyId}} creates it on first
 * call, then updates it (reference {@code get_loyalty_program} / {@code upsert_loyalty_program}).
 */
@RestController
@RequestMapping("/api/crm/loyalty/config")
public class LoyaltyConfigController {

    private final LoyaltyConfigService service;

    public LoyaltyConfigController(LoyaltyConfigService service) {
        this.service = service;
    }

    @GetMapping("/companies/{companyId}")
    @PreAuthorize("hasAuthority('crm.loyaltyconfig.read')")
    public LoyaltyProgramResponse getByCompany(@PathVariable String companyId) {
        return service.getByCompany(companyId);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('crm.loyaltyconfig.read')")
    public LoyaltyProgramResponse get(@PathVariable String publicId) {
        return service.get(publicId);
    }

    @PutMapping
    @PreAuthorize("hasAuthority('crm.loyaltyconfig.write')")
    public LoyaltyProgramResponse upsert(@Valid @RequestBody LoyaltyProgramUpsertRequest request) {
        return service.upsert(request);
    }

    /** FR-206 — add or edit a loyalty tier ({@code id} null = create). */
    @PostMapping("/companies/{companyId}/tiers")
    @PreAuthorize("hasAuthority('crm.loyaltyconfig.write')")
    public LoyaltyProgramResponse upsertTier(@PathVariable String companyId,
                                             @Valid @RequestBody TierUpsertRequest request) {
        return service.upsertTier(companyId, request);
    }

    /** FR-206 — remove a loyalty tier from the program. */
    @DeleteMapping("/companies/{companyId}/tiers/{tierId}")
    @PreAuthorize("hasAuthority('crm.loyaltyconfig.write')")
    public LoyaltyProgramResponse deleteTier(@PathVariable String companyId, @PathVariable String tierId) {
        return service.deleteTier(companyId, tierId);
    }
}

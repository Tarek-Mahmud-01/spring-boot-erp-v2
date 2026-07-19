package com.guru.erp.modules.finance.coa.controller;

import com.guru.erp.modules.finance.coa.domain.AccountModule;
import com.guru.erp.modules.finance.coa.domain.AccountPurpose;
import com.guru.erp.modules.finance.coa.dto.AccountMappingDtos.AccountMappingDeleteResponse;
import com.guru.erp.modules.finance.coa.dto.AccountMappingDtos.AccountMappingMutationResponse;
import com.guru.erp.modules.finance.coa.dto.AccountMappingDtos.AccountMappingResponse;
import com.guru.erp.modules.finance.coa.dto.AccountMappingDtos.AccountMappingUpsertRequest;
import com.guru.erp.modules.finance.coa.dto.AccountMappingDtos.CoaMappingStatusResponse;
import com.guru.erp.modules.finance.coa.service.AccountMappingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Finance &rsaquo; Account Mappings — assign which GL account handles each
 * module/purpose slot (reference {@code views/coa_management.py} AccountMappingView).
 */
@RestController
@RequestMapping("/api/finance")
public class AccountMappingController {

    private final AccountMappingService service;

    public AccountMappingController(AccountMappingService service) {
        this.service = service;
    }

    @GetMapping("/companies/{companyId}/account-mappings")
    @PreAuthorize("hasAuthority('finance.account_mapping.read')")
    public List<AccountMappingResponse> list(@PathVariable String companyId) {
        return service.list(companyId);
    }

    @GetMapping("/companies/{companyId}/account-mappings/status")
    @PreAuthorize("hasAuthority('finance.account_mapping.read')")
    public List<CoaMappingStatusResponse> status(@PathVariable String companyId) {
        return service.mappingStatus(companyId);
    }

    @PostMapping("/account-mappings")
    @PreAuthorize("hasAuthority('finance.account_mapping.write')")
    public AccountMappingMutationResponse upsert(@Valid @RequestBody AccountMappingUpsertRequest request) {
        return service.upsert(request);
    }

    @DeleteMapping("/companies/{companyId}/account-mappings/{module}/{purpose}")
    @PreAuthorize("hasAuthority('finance.account_mapping.write')")
    public AccountMappingDeleteResponse delete(@PathVariable String companyId,
                                               @PathVariable AccountModule module,
                                               @PathVariable AccountPurpose purpose) {
        return service.delete(companyId, module, purpose);
    }
}

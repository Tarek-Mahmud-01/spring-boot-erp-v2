package com.guru.erp.modules.settings.company.controller;

import com.guru.erp.modules.settings.company.dto.CompanyDtos.CompanyCreateRequest;
import com.guru.erp.modules.settings.company.dto.CompanyDtos.CompanyResponse;
import com.guru.erp.modules.settings.company.dto.CompanyDtos.CompanyUpdateRequest;
import com.guru.erp.modules.settings.company.service.CompanyCommandService;
import com.guru.erp.modules.settings.company.service.CompanyQueryService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * ENT-001 Company endpoints (ARCHITECTURE.md §2 — thin controller, @PreAuthorize
 * on every endpoint). Delegates all business rules to the query/command
 * services; exposes ULID public ids only.
 */
@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyQueryService queryService;
    private final CompanyCommandService commandService;

    public CompanyController(CompanyQueryService queryService, CompanyCommandService commandService) {
        this.queryService = queryService;
        this.commandService = commandService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('settings.company.read')")
    public PageResponse<CompanyResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return queryService.list(pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('settings.company.read')")
    public CompanyResponse get(@PathVariable String publicId) {
        return queryService.get(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('settings.company.write')")
    public CompanyResponse create(@Valid @RequestBody CompanyCreateRequest request) {
        return commandService.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('settings.company.write')")
    public CompanyResponse update(@PathVariable String publicId,
                                  @Valid @RequestBody CompanyUpdateRequest request) {
        return commandService.update(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('settings.company.write')")
    public void delete(@PathVariable String publicId) {
        commandService.delete(publicId);
    }

    @PostMapping("/{publicId}/deactivate")
    @PreAuthorize("hasAuthority('settings.company.write')")
    public CompanyResponse deactivate(@PathVariable String publicId) {
        return commandService.deactivate(publicId);
    }

    @PostMapping("/{publicId}/activate")
    @PreAuthorize("hasAuthority('settings.company.write')")
    public CompanyResponse activate(@PathVariable String publicId) {
        return commandService.activate(publicId);
    }
}

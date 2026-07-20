package com.springboot.erp.modules.settings.company.service;

import com.springboot.erp.modules.settings.company.domain.Company;
import com.springboot.erp.modules.settings.company.dto.CompanyDtos.CompanyResponse;
import com.springboot.erp.modules.settings.company.mapper.CompanyMapper;
import com.springboot.erp.modules.settings.company.repository.CompanyRepository;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-side use-cases for ENT-001 Company (ARCHITECTURE.md §3.2 — server-driven
 * lists). Soft-deleted rows are excluded automatically by the entity's
 * {@code @SQLRestriction}.
 */
@Service
public class CompanyQueryService {

    private final CompanyRepository repository;
    private final CompanyMapper mapper;

    public CompanyQueryService(CompanyRepository repository, CompanyMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<CompanyResponse> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CompanyResponse get(String publicId) {
        return mapper.toResponse(load(publicId));
    }

    private Company load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("Company", publicId));
    }
}

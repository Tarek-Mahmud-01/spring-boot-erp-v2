package com.springboot.erp.modules.pos.registers.service;

import com.springboot.erp.modules.pos.registers.dto.RegisterDtos.RegisterResponse;
import com.springboot.erp.modules.pos.registers.mapper.RegistersMapper;
import com.springboot.erp.modules.pos.registers.repository.RegisterRepository;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-side use-cases for ENT-060 Register: list (optionally by location) and get. */
@Service
public class RegisterQueryService {

    private final RegisterRepository repository;
    private final RegistersMapper mapper;

    public RegisterQueryService(RegisterRepository repository, RegistersMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<RegisterResponse> list(String locationId, Pageable pageable) {
        String loc = blankToNull(locationId);
        return PageResponse.of(repository.search(loc, pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RegisterResponse get(String publicId) {
        return mapper.toResponse(repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("Register", publicId)));
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}

package com.guru.erp.modules.finance.gl.service;

import com.guru.erp.modules.finance.gl.dto.GlPostingLogDtos.GlPostingLogResponse;
import com.guru.erp.modules.finance.gl.mapper.GlMapper;
import com.guru.erp.modules.finance.gl.repository.GlPostingLogRepository;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only access to the {@code GlPostingLog} reconciliation feed — "did this event post?". */
@Service
@Transactional(readOnly = true)
public class GlPostingLogQueryService {

    private final GlPostingLogRepository repository;
    private final GlMapper mapper;

    public GlPostingLogQueryService(GlPostingLogRepository repository, GlMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public PageResponse<GlPostingLogResponse> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    public GlPostingLogResponse getBySource(String sourceKind, String sourceRef) {
        return repository.findBySourceKindAndSourceRef(sourceKind, sourceRef)
            .map(mapper::toResponse)
            .orElseThrow(() -> DomainException.notFound("GlPostingLog", sourceKind + "/" + sourceRef));
    }
}

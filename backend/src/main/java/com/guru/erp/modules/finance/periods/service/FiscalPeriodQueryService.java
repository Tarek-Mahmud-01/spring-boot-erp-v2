package com.guru.erp.modules.finance.periods.service;

import com.guru.erp.modules.finance.periods.domain.FiscalPeriod;
import com.guru.erp.modules.finance.periods.domain.FiscalPeriodStatus;
import com.guru.erp.modules.finance.periods.dto.FiscalPeriodDtos.FiscalPeriodResponse;
import com.guru.erp.modules.finance.periods.mapper.FiscalPeriodMapper;
import com.guru.erp.modules.finance.periods.repository.FiscalPeriodRepository;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import com.guru.erp.platform.web.PageResponse;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-side use-cases for {@link FiscalPeriod} (reference {@code fiscal_periods_view.list_fiscal_periods}). */
@Service
@Transactional(readOnly = true)
public class FiscalPeriodQueryService {

    private final FiscalPeriodRepository repository;
    private final FiscalPeriodMapper mapper;

    public FiscalPeriodQueryService(FiscalPeriodRepository repository, FiscalPeriodMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public PageResponse<FiscalPeriodResponse> list(String companyId, String q, FiscalPeriodStatus status,
                                                    LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        return PageResponse.of(repository.search(companyId, q, status, dateFrom, dateTo, pageable), mapper::toResponse);
    }

    public FiscalPeriodResponse get(String publicId) {
        return mapper.toResponse(require(publicId));
    }

    /** Loads the entity (not the DTO) — used by sibling services/controllers in this slice. */
    public FiscalPeriod require(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("FiscalPeriod", publicId));
    }
}

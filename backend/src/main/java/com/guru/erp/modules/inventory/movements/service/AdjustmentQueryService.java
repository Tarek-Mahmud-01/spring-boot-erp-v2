package com.guru.erp.modules.inventory.movements.service;

import com.guru.erp.modules.inventory.movements.domain.AdjustmentStatus;
import com.guru.erp.modules.inventory.movements.dto.AdjustmentDtos.AdjustmentResponse;
import com.guru.erp.modules.inventory.movements.mapper.MovementMapper;
import com.guru.erp.modules.inventory.movements.repository.StockAdjustmentRepository;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-side use-cases for ENT-043 StockAdjustment: list (server-paged + filtered) and get. */
@Service
public class AdjustmentQueryService {

    private final StockAdjustmentRepository repository;
    private final MovementMapper mapper;

    public AdjustmentQueryService(StockAdjustmentRepository repository, MovementMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdjustmentResponse> list(String locationId, String status, String search,
                                                 Pageable pageable) {
        String loc = blankToNull(locationId);
        String q = blankToNull(search);
        AdjustmentStatus st = status == null || status.isBlank()
            ? null : AdjustmentStatus.fromWire(status.trim());
        return PageResponse.of(repository.search(loc, st, q, pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AdjustmentResponse get(String publicId) {
        return mapper.toResponse(repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("StockAdjustment", publicId)));
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}

package com.guru.erp.modules.inventory.movements.service;

import com.guru.erp.modules.inventory.movements.domain.TransferStatus;
import com.guru.erp.modules.inventory.movements.dto.TransferDtos.TransferResponse;
import com.guru.erp.modules.inventory.movements.mapper.MovementMapper;
import com.guru.erp.modules.inventory.movements.repository.StockTransferRepository;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-side use-cases for ENT-042 StockTransfer: list (server-paged + filtered) and get. */
@Service
public class TransferQueryService {

    private final StockTransferRepository repository;
    private final MovementMapper mapper;

    public TransferQueryService(StockTransferRepository repository, MovementMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<TransferResponse> list(String locationId, String status, String search,
                                               Pageable pageable) {
        String loc = blankToNull(locationId);
        String q = blankToNull(search);
        TransferStatus st = status == null || status.isBlank()
            ? null : TransferStatus.fromWire(status.trim());
        return PageResponse.of(repository.search(loc, st, q, pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public TransferResponse get(String publicId) {
        return mapper.toResponse(repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("StockTransfer", publicId)));
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}

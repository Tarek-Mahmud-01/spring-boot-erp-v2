package com.guru.erp.modules.pos.auxiliary.service;

import com.guru.erp.modules.pos.auxiliary.dto.RefundDtos.RefundResponse;
import com.guru.erp.modules.pos.auxiliary.mapper.PosAuxMapper;
import com.guru.erp.modules.pos.auxiliary.repository.PosRefundRepository;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-side use-cases for PosRefund: get by public id, get by refund-transaction id, and list. */
@Service
public class RefundQueryService {

    private final PosRefundRepository repository;
    private final PosAuxMapper mapper;

    public RefundQueryService(PosRefundRepository repository, PosAuxMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public RefundResponse get(String publicId) {
        return mapper.toResponse(repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("PosRefund", publicId)));
    }

    /** Idempotent lookup by the owning REFUND transaction's public id (offline-replay dedupe). */
    @Transactional(readOnly = true)
    public RefundResponse getByTransactionId(String transactionId) {
        return mapper.toResponse(repository.findByTransactionId(transactionId)
            .orElseThrow(() -> DomainException.notFound("PosRefund", transactionId)));
    }

    @Transactional(readOnly = true)
    public PageResponse<RefundResponse> list(String originalTransactionId, Pageable pageable) {
        String orig = originalTransactionId == null || originalTransactionId.isBlank()
            ? null : originalTransactionId.trim();
        return PageResponse.of(repository.search(orig, pageable), mapper::toResponse);
    }
}

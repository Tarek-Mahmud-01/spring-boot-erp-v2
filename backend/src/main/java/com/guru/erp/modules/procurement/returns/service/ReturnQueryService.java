package com.guru.erp.modules.procurement.returns.service;

import com.guru.erp.modules.procurement.returns.domain.ReturnStatus;
import com.guru.erp.modules.procurement.returns.dto.ReturnDtos.ReturnResponse;
import com.guru.erp.modules.procurement.returns.mapper.ReturnMapper;
import com.guru.erp.modules.procurement.returns.repository.SupplierReturnRepository;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-side use-cases for ENT-031 SupplierReturn: list (server-paged + filtered) and get. */
@Service
public class ReturnQueryService {

    private final SupplierReturnRepository repository;
    private final ReturnMapper mapper;

    public ReturnQueryService(SupplierReturnRepository repository, ReturnMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReturnResponse> list(String supplierId, String grnId, String status,
                                             String search, Pageable pageable) {
        String sup = blankToNull(supplierId);
        String grn = blankToNull(grnId);
        String q = blankToNull(search);
        ReturnStatus st = status == null || status.isBlank()
            ? null : ReturnStatus.fromWire(status.trim());
        return PageResponse.of(repository.search(sup, grn, st, q, pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ReturnResponse get(String publicId) {
        return mapper.toResponse(repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("SupplierReturn", publicId)));
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}

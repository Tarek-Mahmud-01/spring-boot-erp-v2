package com.springboot.erp.modules.procurement.orders.service;

import com.springboot.erp.modules.procurement.orders.domain.PrStatus;
import com.springboot.erp.modules.procurement.orders.dto.PrDtos.PrResponse;
import com.springboot.erp.modules.procurement.orders.mapper.OrdersMapper;
import com.springboot.erp.modules.procurement.orders.repository.PurchaseRequisitionRepository;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-side use-cases for ENT-027 PurchaseRequisition: list (server-paged + filtered) and get. */
@Service
public class PurchaseRequisitionQueryService {

    private final PurchaseRequisitionRepository repository;
    private final OrdersMapper mapper;

    public PurchaseRequisitionQueryService(PurchaseRequisitionRepository repository,
                                           OrdersMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<PrResponse> list(String locationId, String status, String search,
                                         Pageable pageable) {
        String loc = blankToNull(locationId);
        String q = blankToNull(search);
        PrStatus st = status == null || status.isBlank() ? null : PrStatus.fromWire(status.trim());
        return PageResponse.of(repository.search(loc, st, q, pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PrResponse get(String publicId) {
        return mapper.toResponse(repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("PurchaseRequisition", publicId)));
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}

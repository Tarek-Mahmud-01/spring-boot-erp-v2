package com.springboot.erp.modules.procurement.orders.service;

import com.springboot.erp.modules.procurement.orders.domain.PoStatus;
import com.springboot.erp.modules.procurement.orders.dto.PoDtos.PoResponse;
import com.springboot.erp.modules.procurement.orders.dto.PoDtos.PoVersionResponse;
import com.springboot.erp.modules.procurement.orders.mapper.OrdersMapper;
import com.springboot.erp.modules.procurement.orders.repository.PurchaseOrderRepository;
import com.springboot.erp.modules.procurement.orders.repository.PurchaseOrderVersionRepository;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.web.PageResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-side use-cases for ENT-028 PurchaseOrder: list (server-paged + filtered), get, and the
 * FR-092 amendment version history.
 */
@Service
public class PurchaseOrderQueryService {

    private final PurchaseOrderRepository repository;
    private final PurchaseOrderVersionRepository versionRepository;
    private final OrdersMapper mapper;

    public PurchaseOrderQueryService(PurchaseOrderRepository repository,
                                     PurchaseOrderVersionRepository versionRepository,
                                     OrdersMapper mapper) {
        this.repository = repository;
        this.versionRepository = versionRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<PoResponse> list(String supplierId, String locationId, String status,
                                         Boolean isDirect, String search, Pageable pageable) {
        String sup = blankToNull(supplierId);
        String loc = blankToNull(locationId);
        String q = blankToNull(search);
        PoStatus st = status == null || status.isBlank() ? null : PoStatus.fromWire(status.trim());
        return PageResponse.of(repository.search(sup, loc, st, isDirect, q, pageable),
            mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PoResponse get(String publicId) {
        return mapper.toResponse(repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("PurchaseOrder", publicId)));
    }

    /** FR-092 — the PO's amendment history, oldest first. */
    @Transactional(readOnly = true)
    public List<PoVersionResponse> versions(String publicId) {
        // Existence check so a bad id 404s rather than returning an empty list.
        repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("PurchaseOrder", publicId));
        return versionRepository.findByPurchaseOrderPublicIdOrderByVersionNoAsc(publicId)
            .stream().map(mapper::toResponse).toList();
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}

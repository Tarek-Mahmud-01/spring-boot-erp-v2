package com.springboot.erp.modules.procurement.suppliers.service;

import com.springboot.erp.modules.procurement.suppliers.domain.SupplierStatus;
import com.springboot.erp.modules.procurement.suppliers.domain.SupplierType;
import com.springboot.erp.modules.procurement.suppliers.dto.SupplierDtos.SupplierResponse;
import com.springboot.erp.modules.procurement.suppliers.mapper.SupplierMapper;
import com.springboot.erp.modules.procurement.suppliers.repository.SupplierRepository;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-side use-cases for ENT-026 Supplier: list (server-paged + filtered) and get by public id. */
@Service
public class SupplierQueryService {

    private final SupplierRepository repository;
    private final SupplierMapper mapper;

    public SupplierQueryService(SupplierRepository repository, SupplierMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<SupplierResponse> list(String status, String type, String locationId,
                                               String search, Pageable pageable) {
        SupplierStatus st = status == null || status.isBlank()
            ? null : SupplierStatus.fromWire(status.trim());
        SupplierType ty = type == null || type.isBlank()
            ? null : SupplierType.valueOf(type.trim().toUpperCase());
        String loc = blankToNull(locationId);
        String q = blankToNull(search);
        return PageResponse.of(repository.search(st, ty, loc, q, pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public SupplierResponse get(String publicId) {
        return mapper.toResponse(repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("Supplier", publicId)));
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}

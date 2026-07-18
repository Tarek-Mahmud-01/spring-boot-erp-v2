package com.guru.erp.modules.procurement.bills.service;

import com.guru.erp.modules.procurement.bills.domain.BillStatus;
import com.guru.erp.modules.procurement.bills.dto.BillDtos.BillResponse;
import com.guru.erp.modules.procurement.bills.mapper.BillMapper;
import com.guru.erp.modules.procurement.bills.repository.SupplierBillRepository;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-side use-cases for ENT-030 SupplierBill: list (server-paged + filtered) and get. */
@Service
public class BillQueryService {

    private final SupplierBillRepository repository;
    private final BillMapper mapper;

    public BillQueryService(SupplierBillRepository repository, BillMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<BillResponse> list(String supplierId, String poId, String status,
                                           String search, Pageable pageable) {
        BillStatus st = status == null || status.isBlank() ? null : BillStatus.fromWire(status.trim());
        return PageResponse.of(
            repository.search(blankToNull(supplierId), blankToNull(poId), st, blankToNull(search), pageable),
            mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public BillResponse get(String publicId) {
        return mapper.toResponse(repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("SupplierBill", publicId)));
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}

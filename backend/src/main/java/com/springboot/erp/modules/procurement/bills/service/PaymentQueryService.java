package com.springboot.erp.modules.procurement.bills.service;

import com.springboot.erp.modules.procurement.bills.domain.SupplierPaymentStatus;
import com.springboot.erp.modules.procurement.bills.dto.PaymentDtos.PaymentResponse;
import com.springboot.erp.modules.procurement.bills.mapper.BillMapper;
import com.springboot.erp.modules.procurement.bills.repository.SupplierPaymentRepository;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-side use-cases for ENT-033 SupplierPayment: list (server-paged + filtered) and get. */
@Service
public class PaymentQueryService {

    private final SupplierPaymentRepository repository;
    private final BillMapper mapper;

    public PaymentQueryService(SupplierPaymentRepository repository, BillMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> list(String supplierId, String poId, String status,
                                              String search, Pageable pageable) {
        SupplierPaymentStatus st = status == null || status.isBlank()
            ? null : SupplierPaymentStatus.fromWire(status.trim());
        return PageResponse.of(
            repository.search(blankToNull(supplierId), blankToNull(poId), st, blankToNull(search), pageable),
            mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(String publicId) {
        return mapper.toResponse(repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("SupplierPayment", publicId)));
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}

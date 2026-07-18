package com.guru.erp.modules.procurement.receipts.service;

import com.guru.erp.modules.procurement.receipts.domain.GrnStatus;
import com.guru.erp.modules.procurement.receipts.dto.ReceiptDtos.GrnResponse;
import com.guru.erp.modules.procurement.receipts.mapper.ReceiptMapper;
import com.guru.erp.modules.procurement.receipts.repository.GoodsReceiptRepository;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-side use-cases for ENT-029 GoodsReceipt: list (server-paged + filtered) and get. */
@Service
public class ReceiptQueryService {

    private final GoodsReceiptRepository repository;
    private final ReceiptMapper mapper;

    public ReceiptQueryService(GoodsReceiptRepository repository, ReceiptMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<GrnResponse> list(String poId, String status, Boolean autoReceipt,
                                          String search, Pageable pageable) {
        String po = blankToNull(poId);
        String q = blankToNull(search);
        GrnStatus st = status == null || status.isBlank() ? null : GrnStatus.fromWire(status.trim());
        return PageResponse.of(repository.search(po, st, autoReceipt, q, pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public GrnResponse get(String publicId) {
        return mapper.toResponse(repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("GoodsReceipt", publicId)));
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}

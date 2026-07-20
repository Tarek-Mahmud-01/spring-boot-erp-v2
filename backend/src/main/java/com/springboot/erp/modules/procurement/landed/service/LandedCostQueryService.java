package com.springboot.erp.modules.procurement.landed.service;

import com.springboot.erp.modules.procurement.landed.domain.LandedCost;
import com.springboot.erp.modules.procurement.landed.dto.LandedCostDtos.LandedCostInvoiceResponse;
import com.springboot.erp.modules.procurement.landed.dto.LandedCostDtos.LandedCostResponse;
import com.springboot.erp.modules.procurement.landed.mapper.LandedMapper;
import com.springboot.erp.modules.procurement.landed.repository.LandedCostRepository;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.web.PageResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-side use-cases for ENT-032 LandedCost: list (server-paged), get, and by-invoice. */
@Service
public class LandedCostQueryService {

    private final LandedCostRepository repository;
    private final LandedMapper mapper;

    public LandedCostQueryService(LandedCostRepository repository, LandedMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<LandedCostResponse> list(String grnId, String search, Pageable pageable) {
        String grn = blankToNull(grnId);
        String q = blankToNull(search);
        return PageResponse.of(repository.search(grn, q, pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public LandedCostResponse get(String publicId) {
        return mapper.toResponse(repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("LandedCost", publicId)));
    }

    /** Every charge row that shares an invoice number (reference by-invoice detail). */
    @Transactional(readOnly = true)
    public List<LandedCostResponse> byInvoice(String invoiceNumber) {
        List<LandedCost> rows = repository.findByInvoiceNumberOrderByIdAsc(invoiceNumber);
        if (rows.isEmpty()) {
            throw DomainException.notFound("LandedCostInvoice", invoiceNumber);
        }
        return rows.stream().map(mapper::toResponse).toList();
    }

    /**
     * One aggregated row per invoice (reference invoice grouping) scoped to a GRN — sums the charge
     * amounts, reports MULTIPLE when the invoice mixes charge types.
     */
    @Transactional(readOnly = true)
    public List<LandedCostInvoiceResponse> listInvoicesForGrn(String grnId) {
        List<LandedCost> rows = repository.findByGrnIdOrderByIdAsc(grnId);
        java.util.Map<String, List<LandedCost>> byInvoice = new java.util.LinkedHashMap<>();
        for (LandedCost lc : rows) {
            String key = lc.getInvoiceNumber() == null ? lc.getPublicId() : lc.getInvoiceNumber();
            byInvoice.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(lc);
        }
        List<LandedCostInvoiceResponse> out = new java.util.ArrayList<>();
        for (List<LandedCost> group : byInvoice.values()) {
            LandedCost first = group.get(0);
            long total = group.stream().mapToLong(lc -> lc.getAmount().amountMinor()).sum();
            boolean multi = group.stream()
                .map(lc -> lc.getChargeType()).distinct().count() > 1;
            out.add(new LandedCostInvoiceResponse(
                first.getPublicId(),
                first.getInvoiceNumber(),
                first.getGrnId(),
                first.getSupplierId(),
                multi ? "MULTIPLE" : first.getChargeType().name(),
                total,
                first.getAmount().currency(),
                first.getAllocationBasis().name(),
                first.getAllocatedAt(),
                group.size()));
        }
        return out;
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}

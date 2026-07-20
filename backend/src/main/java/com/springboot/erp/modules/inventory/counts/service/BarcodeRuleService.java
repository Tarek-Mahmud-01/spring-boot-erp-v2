package com.springboot.erp.modules.inventory.counts.service;

import com.springboot.erp.modules.inventory.counts.domain.BarcodeNomenclatureRule;
import com.springboot.erp.modules.inventory.counts.dto.BarcodeRuleDtos.BarcodeRuleCreateRequest;
import com.springboot.erp.modules.inventory.counts.dto.BarcodeRuleDtos.BarcodeRuleResponse;
import com.springboot.erp.modules.inventory.counts.dto.BarcodeRuleDtos.BarcodeRuleUpdateRequest;
import com.springboot.erp.modules.inventory.counts.mapper.CountsMapper;
import com.springboot.erp.modules.inventory.counts.repository.BarcodeNomenclatureRuleRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ENT-046 BarcodeNomenclatureRule CRUD — parsing rules for scale-printed
 * variable-weight barcodes. Setup / reference data (no lifecycle), so this is a
 * straight create / update / delete / list / get service with one audit row per
 * mutation.
 */
@Service
public class BarcodeRuleService {

    private static final String AUDIT_ENTITY = "barcode_nomenclature_rule";

    private final BarcodeNomenclatureRuleRepository repository;
    private final CountsMapper mapper;
    private final AuditService auditService;

    public BarcodeRuleService(BarcodeNomenclatureRuleRepository repository, CountsMapper mapper,
                              AuditService auditService) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<BarcodeRuleResponse> list(String companyId, Pageable pageable) {
        var page = companyId == null
            ? repository.findAllByOrderByCreatedAtDesc(pageable)
            : repository.findByCompanyIdOrderByCreatedAtDesc(companyId, pageable);
        return PageResponse.of(page, mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public BarcodeRuleResponse get(String publicId) {
        return mapper.toResponse(load(publicId));
    }

    @Transactional
    public BarcodeRuleResponse create(BarcodeRuleCreateRequest req) {
        BarcodeNomenclatureRule r = new BarcodeNomenclatureRule();
        r.setCompanyId(req.companyId());
        r.setName(req.name().strip());
        r.setPrefix(req.prefix().strip());
        r.setRuleType(req.ruleType());
        r.setItemDigits(req.itemDigits());
        r.setMeasureDigits(req.measureDigits());
        r.setMeasureScale(req.measureScale());
        r.setActive(req.isActive() == null || req.isActive());

        BarcodeNomenclatureRule saved = repository.save(r);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public BarcodeRuleResponse update(String publicId, BarcodeRuleUpdateRequest req) {
        BarcodeNomenclatureRule r = load(publicId);
        checkVersion(r, req.version());
        BarcodeRuleResponse before = mapper.toResponse(r);

        if (req.name() != null) {
            r.setName(req.name().strip());
        }
        if (req.prefix() != null) {
            r.setPrefix(req.prefix().strip());
        }
        if (req.ruleType() != null) {
            r.setRuleType(req.ruleType());
        }
        if (req.itemDigits() != null) {
            r.setItemDigits(req.itemDigits());
        }
        if (req.measureDigits() != null) {
            r.setMeasureDigits(req.measureDigits());
        }
        if (req.measureScale() != null) {
            r.setMeasureScale(req.measureScale());
        }
        if (req.isActive() != null) {
            r.setActive(req.isActive());
        }
        BarcodeNomenclatureRule saved = repository.save(r);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(String publicId) {
        BarcodeNomenclatureRule r = load(publicId);
        BarcodeRuleResponse before = mapper.toResponse(r);
        r.softDelete();
        repository.save(r);
        auditService.record(AUDIT_ENTITY, publicId, AuditAction.DELETE, before, null);
    }

    private BarcodeNomenclatureRule load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("BarcodeNomenclatureRule", publicId));
    }

    private void checkVersion(BarcodeNomenclatureRule r, Long requestVersion) {
        if (requestVersion != null && requestVersion != r.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK,
                ErrorCode.OPTIMISTIC_LOCK.defaultDetail());
        }
    }
}

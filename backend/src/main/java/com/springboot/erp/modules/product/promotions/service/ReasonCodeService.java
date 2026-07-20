package com.springboot.erp.modules.product.promotions.service;

import com.springboot.erp.modules.product.promotions.domain.ReasonCode;
import com.springboot.erp.modules.product.promotions.dto.ReasonCodeDtos.ReasonCodeCreateRequest;
import com.springboot.erp.modules.product.promotions.dto.ReasonCodeDtos.ReasonCodeResponse;
import com.springboot.erp.modules.product.promotions.dto.ReasonCodeDtos.ReasonCodeUpdateRequest;
import com.springboot.erp.modules.product.promotions.mapper.ReasonCodeMapper;
import com.springboot.erp.modules.product.promotions.repository.ReasonCodeRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.web.PageResponse;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-068 discount reason-code master use-cases. {@code code} is unique among
 * live rows (case-insensitive) and immutable after creation; a soft-deleted
 * code may be re-introduced. Every mutation records an audit row in-transaction.
 */
@Service
public class ReasonCodeService {

    private static final String ENTITY = "reason_code";

    private final ReasonCodeRepository repository;
    private final ReasonCodeMapper mapper;
    private final AuditService auditService;

    public ReasonCodeService(ReasonCodeRepository repository, ReasonCodeMapper mapper,
                             AuditService auditService) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReasonCodeResponse> list(boolean activeOnly, Pageable pageable) {
        return PageResponse.of(repository.search(activeOnly, pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ReasonCodeResponse get(String publicId) {
        return mapper.toResponse(load(publicId));
    }

    @Transactional
    public ReasonCodeResponse create(ReasonCodeCreateRequest req) {
        String code = req.code().strip();
        if (repository.existsByCode(code, null)) {
            throw new DomainException(ErrorCode.DUPLICATE,
                "Reason code '%s' already exists".formatted(code), Map.of("code", code));
        }
        ReasonCode rc = new ReasonCode();
        rc.setCode(code);
        rc.setLabel(req.label().strip());
        rc.setDescription(req.description() == null ? null : blankToNull(req.description().strip()));
        rc.setActive(req.isActive() == null || req.isActive());

        ReasonCode saved = repository.save(rc);
        auditService.record(ENTITY, saved.getPublicId(), AuditAction.CREATE, null, snapshot(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public ReasonCodeResponse update(String publicId, ReasonCodeUpdateRequest req) {
        ReasonCode rc = load(publicId);
        assertVersion(rc, req.version());
        Map<String, Object> before = snapshot(rc);

        if (req.label() != null) {
            rc.setLabel(req.label().strip());
        }
        if (req.description() != null) {
            rc.setDescription(blankToNull(req.description().strip()));
        }
        if (req.isActive() != null) {
            rc.setActive(req.isActive());
        }
        ReasonCode saved = repository.save(rc);
        auditService.record(ENTITY, saved.getPublicId(), AuditAction.UPDATE, before, snapshot(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(String publicId) {
        ReasonCode rc = load(publicId);
        Map<String, Object> before = snapshot(rc);
        rc.softDelete();
        repository.save(rc);
        auditService.record(ENTITY, rc.getPublicId(), AuditAction.DELETE, before, null);
    }

    // --- helpers ---

    private ReasonCode load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("ReasonCode", publicId));
    }

    private void assertVersion(ReasonCode rc, Long expected) {
        if (expected != null && expected != rc.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK, "Reason code was modified concurrently",
                Map.of("expected", expected, "actual", rc.getVersion()));
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }

    private Map<String, Object> snapshot(ReasonCode rc) {
        return Map.of(
            "id", rc.getPublicId(),
            "code", rc.getCode(),
            "label", rc.getLabel(),
            "description", rc.getDescription() == null ? "" : rc.getDescription(),
            "isActive", rc.isActive(),
            "version", rc.getVersion());
    }
}

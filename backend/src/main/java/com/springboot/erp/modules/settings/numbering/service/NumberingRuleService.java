package com.springboot.erp.modules.settings.numbering.service;

import com.springboot.erp.modules.settings.numbering.domain.DocumentType;
import com.springboot.erp.modules.settings.numbering.domain.NumberingRule;
import com.springboot.erp.modules.settings.numbering.dto.NumberingRuleDtos.NumberingAllocateRequest;
import com.springboot.erp.modules.settings.numbering.dto.NumberingRuleDtos.NumberingAllocateResponse;
import com.springboot.erp.modules.settings.numbering.dto.NumberingRuleDtos.NumberingRuleCreateRequest;
import com.springboot.erp.modules.settings.numbering.dto.NumberingRuleDtos.NumberingRuleResponse;
import com.springboot.erp.modules.settings.numbering.dto.NumberingRuleDtos.NumberingRuleUpdateRequest;
import com.springboot.erp.modules.settings.numbering.mapper.NumberingRuleMapper;
import com.springboot.erp.modules.settings.numbering.repository.NumberingRuleRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business rules for ENT-006 Numbering Rule (US-005 / FR-022-025). Holds
 * uniqueness (one rule per company+document type), the FR-025 lock (once a
 * number has been issued only {@code padding} may change, and only upward),
 * optimistic-lock reconciliation, and the FR-023/024 sequence allocation.
 *
 * <p>Every mutation records an audit row inside its own transaction.
 *
 * <p>There is no DELETE: once a rule has issued a number the audit trail requires
 * the row to stay (see reference {@code detail.py}). Kept under the 250-line cap.
 */
@Service
public class NumberingRuleService {

    private static final String AUDIT_ENTITY = "numbering_rule";

    /**
     * Fiscal year start (MM-DD) used for YEARLY window keys. The reference reads
     * this from the owning Company; v2 has no Company entity yet, so we default to
     * the calendar year. Revisit when companies are ported.
     */
    private static final String DEFAULT_FISCAL_YEAR_START = "01-01";

    private final NumberingRuleRepository repository;
    private final NumberingRuleMapper mapper;
    private final AuditService auditService;

    public NumberingRuleService(NumberingRuleRepository repository, NumberingRuleMapper mapper,
                                AuditService auditService) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<NumberingRuleResponse> list(String companyId, DocumentType documentType,
                                                    Pageable pageable) {
        var page = query(companyId, documentType, pageable);
        return PageResponse.of(page, mapper::toResponse);
    }

    private org.springframework.data.domain.Page<NumberingRule> query(
        String companyId, DocumentType documentType, Pageable pageable) {
        if (companyId != null && documentType != null) {
            return repository.findByCompanyIdAndDocumentType(companyId, documentType, pageable);
        }
        if (companyId != null) {
            return repository.findByCompanyId(companyId, pageable);
        }
        if (documentType != null) {
            return repository.findByDocumentType(documentType, pageable);
        }
        return repository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public NumberingRuleResponse get(String publicId) {
        return mapper.toResponse(load(publicId));
    }

    /** FR-022 / AC-005-1: create a rule; rejects a duplicate (company, document type). */
    @Transactional
    public NumberingRuleResponse create(NumberingRuleCreateRequest req) {
        if (repository.existsByCompanyIdAndDocumentType(req.companyId(), req.documentType())) {
            throw new DomainException(ErrorCode.DUPLICATE,
                "A numbering rule already exists for this company and document type");
        }
        NumberingRule rule = NumberingRule.create(
            req.companyId(), req.documentType(), req.prefix(),
            req.padding(), req.resetCadence(), req.startValue());
        rule = repository.save(rule);
        auditService.record(AUDIT_ENTITY, rule.getPublicId(), AuditAction.CREATE, null,
            mapper.toResponse(rule));
        return mapper.toResponse(rule);
    }

    /**
     * FR-025 / AC-005-3: update a rule. Once it has issued a number it is locked:
     * only {@code padding} may change, and only to a larger value.
     */
    @Transactional
    public NumberingRuleResponse update(String publicId, NumberingRuleUpdateRequest req) {
        NumberingRule rule = load(publicId);
        assertVersion(rule, req.version());
        NumberingRuleResponse before = mapper.toResponse(rule);

        if (rule.isLocked()) {
            boolean touchesLockedField = req.startValue() != null
                || req.resetCadence() != null
                || req.prefix() != null;
            if (touchesLockedField) {
                throw new DomainException(ErrorCode.CONFLICT,
                    "Numbering rule has issued numbers; only padding may be extended");
            }
            if (req.padding() != null && req.padding() < rule.getPadding()) {
                throw new DomainException(ErrorCode.CONFLICT,
                    "Padding can only be extended, not reduced");
            }
        }

        if (req.prefix() != null) {
            rule.changePrefix(req.prefix());
        }
        if (req.padding() != null) {
            rule.changePadding(req.padding());
        }
        if (req.resetCadence() != null) {
            rule.changeResetCadence(req.resetCadence());
        }
        if (req.startValue() != null) {
            rule.changeStartValue(req.startValue());
        }

        NumberingRuleResponse after = mapper.toResponse(rule);
        auditService.record(AUDIT_ENTITY, rule.getPublicId(), AuditAction.UPDATE, before, after);
        return after;
    }

    /**
     * FR-023 / FR-024 / AC-005-1/2/4: consume one number for {@code documentDate}.
     * If the document date falls in a new window the counter rewinds first. This is
     * an irreversible mutation and is audited.
     */
    @Transactional
    public NumberingAllocateResponse allocate(String publicId, NumberingAllocateRequest req) {
        NumberingRule rule = load(publicId);
        String windowKey = rule.windowKeyFor(DEFAULT_FISCAL_YEAR_START, req.documentDate());

        var before = new AllocationSnapshot(rule.getCurrentValue(), rule.getCurrentWindowKey());
        long value = rule.advance(windowKey);
        String number = rule.formatNumber(windowKey, value);

        auditService.record(AUDIT_ENTITY, rule.getPublicId(), AuditAction.UPDATE, before,
            new AllocationResult(value, windowKey, number));
        return new NumberingAllocateResponse(number, value, windowKey);
    }

    private NumberingRule load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("NumberingRule", publicId));
    }

    /** Optimistic-lock guard: the client's expected version must match the row's. */
    private void assertVersion(NumberingRule rule, Long expectedVersion) {
        if (expectedVersion != null && expectedVersion != rule.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK,
                "Numbering Rule was modified concurrently");
        }
    }

    private record AllocationSnapshot(long currentValue, String currentWindowKey) {
    }

    private record AllocationResult(long currentValue, String currentWindowKey, String number) {
    }
}

package com.guru.erp.modules.settings.taxcode.service;

import com.guru.erp.modules.settings.taxcode.domain.GstTreatment;
import com.guru.erp.modules.settings.taxcode.domain.TaxCode;
import com.guru.erp.modules.settings.taxcode.dto.TaxCodeDtos.TaxCodeCreateRequest;
import com.guru.erp.modules.settings.taxcode.dto.TaxCodeDtos.TaxCodeResponse;
import com.guru.erp.modules.settings.taxcode.dto.TaxCodeDtos.TaxCodeUpdateRequest;
import com.guru.erp.modules.settings.taxcode.mapper.TaxCodeMapper;
import com.guru.erp.modules.settings.taxcode.repository.TaxCodeRepository;
import com.guru.erp.platform.audit.AuditAction;
import com.guru.erp.platform.audit.AuditService;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import com.guru.erp.platform.web.PageResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ENT-007 Tax Code use-cases (FR-012-016). All mutations run in a transaction,
 * enforce the validation and overlap invariants ported from the reference
 * views, and append an audit row.
 *
 * <p>Ported business rules:
 * <ul>
 *   <li>rate_percent in [0, 100] and effective_to &gt;= effective_from (also DB checks).</li>
 *   <li>No two live ranges for the same {@code (company, code)} may overlap
 *       (AC-003-2 — pure-Java mirror of the Postgres GIST exclude).</li>
 *   <li>Optimistic-lock on update via {@code request.version}.</li>
 *   <li>Derived status = effective_to closed-in-past ? "Inactive" : "Active".</li>
 *   <li>Soft delete (BaseEntity) rather than hard delete, preserving the audit trail.</li>
 * </ul>
 */
@Service
public class TaxCodeService {

    private static final String AUDIT_ENTITY = "tax_code";
    private static final LocalDate OPEN_ENDED = LocalDate.of(9999, 12, 31);

    private final TaxCodeRepository repository;
    private final TaxCodeMapper mapper;
    private final AuditService auditService;

    public TaxCodeService(TaxCodeRepository repository, TaxCodeMapper mapper, AuditService auditService) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<TaxCodeResponse> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public TaxCodeResponse get(String publicId) {
        return mapper.toResponse(load(publicId));
    }

    @Transactional
    public TaxCodeResponse create(TaxCodeCreateRequest req) {
        String code = req.code().strip();
        LocalDate from = req.effectiveFrom();
        LocalDate to = req.effectiveTo();
        assertEffectiveRange(from, to);

        GstTreatment treatment = req.gstTreatment() == null ? GstTreatment.STANDARD : req.gstTreatment();
        assertNoOverlap(req.companyId(), code, from, to, null);

        TaxCode entity = new TaxCode(
            req.companyId(), code, req.description().strip(), req.ratePercent(),
            req.inclusive(), req.exempt(), treatment, from, to);
        TaxCode saved = repository.save(entity);

        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public TaxCodeResponse update(String publicId, TaxCodeUpdateRequest req) {
        TaxCode entity = load(publicId);
        if (req.version() != null && req.version() != entity.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK,
                "Tax Code was modified concurrently");
        }
        TaxCodeResponse before = mapper.toResponse(entity);

        LocalDate proposedFrom = req.effectiveFrom() != null ? req.effectiveFrom() : entity.getEffectiveFrom();
        LocalDate proposedTo = req.effectiveTo() != null ? req.effectiveTo() : entity.getEffectiveTo();
        boolean rangeChanged = !proposedFrom.equals(entity.getEffectiveFrom())
            || !java.util.Objects.equals(proposedTo, entity.getEffectiveTo());

        if (rangeChanged) {
            assertEffectiveRange(proposedFrom, proposedTo);
            assertNoOverlap(entity.getCompanyPublicId(), entity.getCode(), proposedFrom, proposedTo,
                entity.getId());
        }

        if (req.description() != null) {
            entity.setDescription(req.description().strip());
        }
        if (req.ratePercent() != null) {
            entity.setRatePercent(req.ratePercent());
        }
        if (req.inclusive() != null) {
            entity.setInclusive(req.inclusive());
        }
        if (req.exempt() != null) {
            entity.setExempt(req.exempt());
        }
        if (req.gstTreatment() != null) {
            entity.setGstTreatment(req.gstTreatment());
        }
        if (req.effectiveFrom() != null) {
            entity.setEffectiveFrom(req.effectiveFrom());
        }
        if (req.effectiveTo() != null) {
            entity.setEffectiveTo(req.effectiveTo());
        }

        TaxCode saved = repository.save(entity);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(String publicId) {
        TaxCode entity = load(publicId);
        TaxCodeResponse before = mapper.toResponse(entity);
        entity.softDelete();
        repository.save(entity);
        auditService.record(AUDIT_ENTITY, entity.getPublicId(), AuditAction.DELETE, before, null);
    }

    private TaxCode load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("TaxCode", publicId));
    }

    private void assertEffectiveRange(LocalDate from, LocalDate to) {
        if (to != null && to.isBefore(from)) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "effective_to must be on or after effective_from");
        }
    }

    /**
     * FR-013 / AC-003-2: reject a new/updated range whose interval overlaps any
     * other live range for the same {@code (company, code)}. Inclusive on both
     * bounds; a null upper bound is treated as open-ended.
     */
    private void assertNoOverlap(String companyPublicId, String code, LocalDate from, LocalDate to,
                                 Long excludeId) {
        LocalDate upper = to == null ? OPEN_ENDED : to;
        List<TaxCode> existing = repository.findByCompanyPublicIdAndCode(companyPublicId, code);
        for (TaxCode e : existing) {
            if (excludeId != null && excludeId.equals(e.getId())) {
                continue;
            }
            LocalDate eUpper = e.getEffectiveTo() == null ? OPEN_ENDED : e.getEffectiveTo();
            boolean overlaps = !from.isAfter(eUpper) && !e.getEffectiveFrom().isAfter(upper);
            if (overlaps) {
                throw new DomainException(ErrorCode.CONFLICT,
                    "Overlapping effective range for tax code '%s'".formatted(code),
                    java.util.Map.of("existingTaxCodeId", e.getPublicId()));
            }
        }
    }
}

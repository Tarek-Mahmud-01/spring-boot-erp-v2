package com.guru.erp.modules.product.promotions.service;

import com.guru.erp.modules.product.promotions.domain.Promotion;
import com.guru.erp.modules.product.promotions.domain.PromotionStatus;
import com.guru.erp.modules.product.promotions.domain.PromotionType;
import com.guru.erp.modules.product.promotions.dto.PromotionDtos.PromotionCreateRequest;
import com.guru.erp.modules.product.promotions.dto.PromotionDtos.PromotionResponse;
import com.guru.erp.modules.product.promotions.dto.PromotionDtos.PromotionStatusUpdateRequest;
import com.guru.erp.modules.product.promotions.dto.PromotionDtos.PromotionUpdateRequest;
import com.guru.erp.modules.product.promotions.mapper.PromotionMapper;
import com.guru.erp.modules.product.promotions.repository.PromotionRepository;
import com.guru.erp.platform.audit.AuditAction;
import com.guru.erp.platform.audit.AuditService;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import com.guru.erp.platform.web.PageResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ENT-014 Promotion use-cases (US-014 / FR-065..069). Reproduces the reference
 * rules: type-specific config validation, {@code date_to > date_from}, refusal
 * of an already-closed window at create / on activation, per-company
 * case-insensitive name uniqueness, and the FR-069 post-sale discount-rule lock.
 * Every mutation records an audit row in-transaction.
 *
 * <p>The FR-069 lock depends on POS ("was this promotion applied to a completed
 * sale?"), which is a different epic. Until a {@code PromotionUsageProvider} is
 * wired, no promotion is ever used, so the rule-change lock is a no-op — exactly
 * the reference behaviour when the provider is unregistered.
 */
@Service
public class PromotionService {

    private static final String ENTITY = "promotion";

    private final PromotionRepository repository;
    private final PromotionMapper mapper;
    private final AuditService auditService;

    public PromotionService(PromotionRepository repository, PromotionMapper mapper,
                            AuditService auditService) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<PromotionResponse> list(String companyId, Pageable pageable) {
        String company = (companyId == null || companyId.isBlank()) ? null : companyId;
        return PageResponse.of(repository.search(company, pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PromotionResponse get(String publicId) {
        return mapper.toResponse(load(publicId));
    }

    @Transactional
    public PromotionResponse create(PromotionCreateRequest req) {
        Instant df = req.dateFrom();
        Instant dt = req.dateTo();
        if (!dt.isAfter(df)) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "date_to must be after date_from",
                Map.of("field", "date_to"));
        }
        if (!dt.isAfter(Instant.now())) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Promotion window has already closed", Map.of("field", "date_to"));
        }
        PromotionConfigValidator.validate(req.type(), req.config());
        String name = req.name().strip();
        assertNameAvailable(req.companyId(), name, null);

        Promotion p = new Promotion();
        p.setCompanyId(req.companyId());
        p.setName(name);
        p.setType(req.type());
        p.setConfig(req.config());
        p.setScope(req.scope() == null ? Map.of() : req.scope());
        p.setSegmentId(req.segmentId());
        p.setDateFrom(df);
        p.setDateTo(dt);
        p.setStackable(Boolean.TRUE.equals(req.stackable()));
        p.setReasonRequired(Boolean.TRUE.equals(req.reasonRequired()));
        p.setStatus(req.status() == null ? PromotionStatus.ACTIVE : req.status());

        Promotion saved = repository.save(p);
        auditService.record(ENTITY, saved.getPublicId(), AuditAction.CREATE, null,
            PromotionSnapshot.of(saved));
        return mapper.toResponse(saved, overlapWarnings(saved));
    }

    @Transactional
    public PromotionResponse update(String publicId, PromotionUpdateRequest req) {
        Promotion p = load(publicId);
        assertVersion(p, req.version());
        Map<String, Object> before = PromotionSnapshot.of(p);

        // FR-069 — the discount rule (type/config/window) freezes once the
        // promotion has priced a completed sale. No POS provider is wired yet,
        // so usage is always false and this never blocks (reference parity).
        boolean ruleChange = req.type() != null || req.config() != null
            || req.dateFrom() != null || req.dateTo() != null;
        if (ruleChange && usedInCompletedSale(p)) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Promotion discount rule is locked: it has priced a completed sale",
                Map.of("promotionId", p.getPublicId()));
        }

        if (req.type() != null || req.config() != null) {
            PromotionType effType = req.type() != null ? req.type() : p.getType();
            Map<String, Object> effConfig = req.config() != null ? req.config() : p.getConfig();
            PromotionConfigValidator.validate(effType, effConfig);
        }
        Instant effFrom = req.dateFrom() != null ? req.dateFrom() : p.getDateFrom();
        Instant effTo = req.dateTo() != null ? req.dateTo() : p.getDateTo();
        if ((req.dateFrom() != null || req.dateTo() != null) && !effTo.isAfter(effFrom)) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "date_to must be after date_from",
                Map.of("field", "date_to"));
        }
        PromotionStatus effStatus = req.status() != null ? req.status() : p.getStatus();
        if (effStatus == PromotionStatus.ACTIVE && !effTo.isAfter(Instant.now())) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Promotion window has already closed", Map.of("field", "status"));
        }
        if (req.name() != null && !req.name().strip().equalsIgnoreCase(p.getName())) {
            assertNameAvailable(p.getCompanyId(), req.name().strip(), p.getId());
        }

        if (req.name() != null) {
            p.setName(req.name().strip());
        }
        if (req.type() != null) {
            p.setType(req.type());
        }
        if (req.config() != null) {
            p.setConfig(req.config());
        }
        if (req.scope() != null) {
            p.setScope(req.scope());
        }
        if (req.dateFrom() != null) {
            p.setDateFrom(effFrom);
        }
        if (req.dateTo() != null) {
            p.setDateTo(effTo);
        }
        if (req.stackable() != null) {
            p.setStackable(req.stackable());
        }
        if (req.reasonRequired() != null) {
            p.setReasonRequired(req.reasonRequired());
        }
        if (req.segmentId() != null) {
            p.setSegmentId(req.segmentId());
        }
        if (req.status() != null) {
            p.setStatus(req.status());
        }

        Promotion saved = repository.save(p);
        auditService.record(ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            PromotionSnapshot.of(saved));
        return mapper.toResponse(saved, overlapWarnings(saved));
    }

    @Transactional
    public PromotionResponse updateStatus(String publicId, PromotionStatusUpdateRequest req) {
        Promotion p = load(publicId);
        assertVersion(p, req.version());
        // Refuse to (re)activate a promotion whose window has already closed.
        if (req.status() == PromotionStatus.ACTIVE && !p.getDateTo().isAfter(Instant.now())) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Promotion window has already closed", Map.of("field", "status"));
        }
        Map<String, Object> before = Map.of("status", p.getStatus().name());
        p.setStatus(req.status());
        Promotion saved = repository.save(p);
        auditService.record(ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            Map.of("status", saved.getStatus().name()));
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(String publicId) {
        Promotion p = load(publicId);
        Map<String, Object> before = PromotionSnapshot.of(p);
        p.softDelete();
        repository.save(p);
        auditService.record(ENTITY, p.getPublicId(), AuditAction.DELETE, before, null);
    }

    // --- helpers ---

    private Promotion load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("Promotion", publicId));
    }

    private void assertNameAvailable(String companyId, String name, Long excludeId) {
        if (repository.existsByCompanyAndName(companyId, name, excludeId)) {
            throw new DomainException(ErrorCode.DUPLICATE,
                "A promotion named '%s' already exists for this company".formatted(name),
                Map.of("name", name));
        }
    }

    private void assertVersion(Promotion p, Long expected) {
        if (expected != null && expected != p.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK, "Promotion was modified concurrently",
                Map.of("expected", expected, "actual", p.getVersion()));
        }
    }

    /** FR-069 seam — no POS usage provider is wired in this slice, so always false. */
    private boolean usedInCompletedSale(Promotion p) {
        return false;
    }

    /** FR-066 — non-blocking advisories: other ACTIVE, window-overlapping promotions on an overlapping scope. */
    private List<String> overlapWarnings(Promotion promo) {
        if (promo.getStatus() != PromotionStatus.ACTIVE) {
            return List.of();
        }
        List<String> names = repository
            .search(promo.getCompanyId(), Pageable.unpaged())
            .getContent().stream()
            .filter(o -> !o.getId().equals(promo.getId()))
            .filter(o -> o.getStatus() == PromotionStatus.ACTIVE)
            .filter(o -> o.getDateFrom().isBefore(promo.getDateTo())
                && o.getDateTo().isAfter(promo.getDateFrom()))
            .filter(o -> PromotionOverlap.scopesOverlap(promo.getScope(), o.getScope()))
            .map(Promotion::getName)
            .toList();
        return PromotionOverlap.warnings(names);
    }
}

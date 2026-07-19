package com.guru.erp.modules.crm.loyalty.service;

import com.guru.erp.modules.crm.loyalty.domain.LoyaltyConfig;
import com.guru.erp.modules.crm.loyalty.dto.LoyaltyConfigDtos.LoyaltyProgramResponse;
import com.guru.erp.modules.crm.loyalty.dto.LoyaltyConfigDtos.LoyaltyProgramUpsertRequest;
import com.guru.erp.modules.crm.loyalty.dto.LoyaltyConfigDtos.LoyaltyTier;
import com.guru.erp.modules.crm.loyalty.dto.LoyaltyConfigDtos.TierUpsertRequest;
import com.guru.erp.modules.crm.loyalty.mapper.LoyaltyMapper;
import com.guru.erp.modules.crm.loyalty.repository.LoyaltyConfigRepository;
import com.guru.erp.platform.audit.AuditAction;
import com.guru.erp.platform.audit.AuditService;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import com.guru.erp.platform.id.Ulid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write + point-read use-cases for the per-company {@link LoyaltyConfig}
 * singleton (reference {@code get_loyalty_program} / {@code upsert_loyalty_program}
 * / {@code upsert_tier} / {@code delete_tier}, US-039 / FR-205-207). One config
 * per company: the first upsert creates it, later calls update it — mirroring
 * {@code ValuationConfigService}'s create-on-first-call pattern.
 */
@Service
public class LoyaltyConfigService {

    static final String AUDIT_ENTITY = "loyalty_config";

    private final LoyaltyConfigRepository repository;
    private final LoyaltyMapper mapper;
    private final AuditService auditService;

    public LoyaltyConfigService(LoyaltyConfigRepository repository, LoyaltyMapper mapper,
                                AuditService auditService) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public LoyaltyProgramResponse getByCompany(String companyId) {
        return mapper.toResponse(loadByCompany(companyId));
    }

    @Transactional(readOnly = true)
    public LoyaltyProgramResponse get(String publicId) {
        return mapper.toResponse(load(publicId));
    }

    /** US-039 — create or update the company's single loyalty program. */
    @Transactional
    public LoyaltyProgramResponse upsert(LoyaltyProgramUpsertRequest req) {
        LoyaltyConfig cfg = repository.findByCompanyId(req.companyId()).orElse(null);
        boolean created = cfg == null;
        if (created) {
            // FR-205 — name is required when creating the program for the first time.
            if (req.name() == null || req.name().isBlank()) {
                throw new DomainException(ErrorCode.VALIDATION_FAILED,
                    "Loyalty program name is required when creating the program");
            }
            cfg = new LoyaltyConfig();
            cfg.setCompanyId(req.companyId());
            cfg.setName(req.name());
            cfg.setActive(req.active() == null || req.active());
            cfg.setCurrency((req.currency() != null ? req.currency() : "USD").toUpperCase());
            cfg.setEarnRule(Map.of("currencyUnitPerPoint", 1.0, "eligibleCategoryIds", List.of()));
            cfg.setRedeemRule(Map.of("pointsPerCurrencyUnit", 100, "minBalanceForRedemption", 0));
            cfg.setExpiryMonths(req.expiryMonths() != null ? req.expiryMonths() : 24);
            cfg.setTiers(List.of());
        }
        LoyaltyProgramResponse before = created ? null : mapper.toResponse(cfg);

        if (req.name() != null) {
            cfg.setName(req.name());
        }
        if (req.active() != null) {
            cfg.setActive(req.active());
        }
        if (req.currency() != null) {
            cfg.setCurrency(req.currency().toUpperCase());
        }
        if (req.earn() != null) {
            cfg.setEarnRule(Map.of(
                "currencyUnitPerPoint", req.earn().currencyUnitPerPoint(),
                "eligibleCategoryIds", req.earn().eligibleCategoryIds() != null
                    ? req.earn().eligibleCategoryIds() : List.of()));
        }
        if (req.redeem() != null) {
            cfg.setRedeemRule(Map.of(
                "pointsPerCurrencyUnit", req.redeem().pointsPerCurrencyUnit(),
                "minBalanceForRedemption", req.redeem().minBalanceForRedemption()));
        }
        if (req.expiryMonths() != null) {
            cfg.setExpiryMonths(req.expiryMonths());
        }

        LoyaltyConfig saved = repository.save(cfg);
        LoyaltyProgramResponse after = mapper.toResponse(saved);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(),
            created ? AuditAction.CREATE : AuditAction.UPDATE, before, after);
        return after;
    }

    /** FR-206 — add or edit a loyalty tier. Tiers are one JSON list on the config row. */
    @Transactional
    public LoyaltyProgramResponse upsertTier(String companyId, TierUpsertRequest req) {
        LoyaltyConfig cfg = loadByCompany(companyId);
        LoyaltyProgramResponse before = mapper.toResponse(cfg);

        List<Map<String, Object>> tiers = new ArrayList<>(cfg.getTiers());
        String normalisedCode = req.code().strip().toUpperCase();
        for (Map<String, Object> t : tiers) {
            String existingId = (String) t.get("id");
            if (existingId.equals(req.id())) {
                continue;
            }
            // FR-206 — tier code must be unique within the program.
            if (String.valueOf(t.get("code")).strip().toUpperCase().equals(normalisedCode)) {
                throw new DomainException(ErrorCode.DUPLICATE, "Tier code already exists: " + req.code());
            }
            // FR-206 — tier threshold must also be unique; overlapping thresholds make
            // tier assignment ambiguous.
            if (((Number) t.get("minSpendAmount")).longValue() == req.minSpendAmount()) {
                throw new DomainException(ErrorCode.DUPLICATE,
                    "A tier with this min spend threshold already exists");
            }
        }

        LoyaltyTier tier = new LoyaltyTier(
            req.id() != null ? req.id() : Ulid.next(),
            req.code(), req.name(), req.minSpendAmount(), req.currency().toUpperCase(), req.earnMultiplier());
        Map<String, Object> tierMap = mapper.toMap(tier);

        boolean replaced = false;
        List<Map<String, Object>> next = new ArrayList<>();
        for (Map<String, Object> t : tiers) {
            if (req.id() != null && req.id().equals(t.get("id"))) {
                next.add(tierMap);
                replaced = true;
            } else {
                next.add(t);
            }
        }
        if (!replaced) {
            next.add(tierMap);
        }
        // Keep tiers ordered by threshold so tier resolution is a simple scan.
        next.sort((a, b) -> Long.compare(((Number) a.get("minSpendAmount")).longValue(),
            ((Number) b.get("minSpendAmount")).longValue()));
        cfg.setTiers(next);

        LoyaltyConfig saved = repository.save(cfg);
        LoyaltyProgramResponse after = mapper.toResponse(saved);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before, after);
        return after;
    }

    /** FR-206 — remove a loyalty tier from the program. */
    @Transactional
    public LoyaltyProgramResponse deleteTier(String companyId, String tierId) {
        LoyaltyConfig cfg = loadByCompany(companyId);
        LoyaltyProgramResponse before = mapper.toResponse(cfg);

        List<Map<String, Object>> tiers = cfg.getTiers();
        if (tiers.stream().noneMatch(t -> tierId.equals(t.get("id")))) {
            throw DomainException.notFound("LoyaltyTier", tierId);
        }
        cfg.setTiers(tiers.stream().filter(t -> !tierId.equals(t.get("id"))).toList());

        LoyaltyConfig saved = repository.save(cfg);
        LoyaltyProgramResponse after = mapper.toResponse(saved);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before, after);
        return after;
    }

    LoyaltyConfig loadByCompany(String companyId) {
        return repository.findByCompanyId(companyId)
            .orElseThrow(() -> DomainException.notFound("LoyaltyConfig", companyId));
    }

    private LoyaltyConfig load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("LoyaltyConfig", publicId));
    }
}

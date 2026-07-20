package com.springboot.erp.modules.settings.currency.service;

import com.springboot.erp.modules.settings.currency.domain.Currency;
import com.springboot.erp.modules.settings.currency.dto.CurrencyDtos.CurrencyCreateRequest;
import com.springboot.erp.modules.settings.currency.dto.CurrencyDtos.CurrencyResponse;
import com.springboot.erp.modules.settings.currency.dto.CurrencyDtos.CurrencyUpdateRequest;
import com.springboot.erp.modules.settings.currency.mapper.CurrencyMapper;
import com.springboot.erp.modules.settings.currency.repository.CurrencyRepository;
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
 * ENT-018 Currency use-cases. Holds every business rule ported from the
 * reference views: ISO-code normalisation + uniqueness, the auto-first-default
 * invariant, immutable {@code code}, refusal to deactivate/delete the default,
 * the in-use delete guard, the transactional-history lock on default swaps, and
 * optimistic-lock checks. Every mutation records an audit row in-transaction.
 *
 * <p>Kept under the 250-line cap.
 */
@Service
public class CurrencyService {

    private static final String ENTITY = "currency";

    private final CurrencyRepository repository;
    private final CurrencyMapper mapper;
    private final AuditService auditService;
    private final CurrencyReferenceGuard referenceGuard;

    public CurrencyService(CurrencyRepository repository, CurrencyMapper mapper,
                           AuditService auditService, CurrencyReferenceGuard referenceGuard) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.referenceGuard = referenceGuard;
    }

    @Transactional(readOnly = true)
    public PageResponse<CurrencyResponse> list(String q, String status, Pageable pageable) {
        Boolean active = switch (status == null ? "" : status) {
            case "active" -> Boolean.TRUE;
            case "inactive" -> Boolean.FALSE;
            default -> null;
        };
        String query = (q == null || q.isBlank()) ? null : q.trim();
        return PageResponse.of(repository.search(query, active, pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CurrencyResponse get(String publicId) {
        return mapper.toResponse(load(publicId));
    }

    @Transactional
    public CurrencyResponse create(CurrencyCreateRequest req) {
        String code = req.code().toUpperCase();
        if (repository.existsByCode(code)) {
            throw new DomainException(ErrorCode.DUPLICATE,
                "Currency code '%s' already exists".formatted(code),
                Map.of("currencyCode", code));
        }
        // The first currency created is auto-marked default so there is always
        // exactly one. Subsequent rows default to non-default (promote via
        // set-default).
        boolean isDefault = repository.count() == 0;

        Currency c = new Currency(
            code,
            req.name().strip(),
            req.shortName().strip(),
            req.country().strip(),
            req.symbol().strip(),
            req.decimalPlaces() == null ? 2 : req.decimalPlaces(),
            req.isActive() == null || req.isActive(),
            isDefault);

        Currency saved = repository.save(c);
        auditService.record(ENTITY, saved.getPublicId(), AuditAction.CREATE, null, snapshot(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public CurrencyResponse update(String publicId, CurrencyUpdateRequest req) {
        Currency c = load(publicId);
        assertVersion(c, req.version());
        Map<String, Object> before = snapshot(c);

        if (req.name() != null) {
            c.setName(req.name().strip());
        }
        if (req.shortName() != null) {
            c.setShortName(req.shortName().strip());
        }
        if (req.country() != null) {
            c.setCountry(req.country().strip());
        }
        if (req.symbol() != null) {
            c.setSymbol(req.symbol().strip());
        }
        if (req.decimalPlaces() != null) {
            c.setDecimalPlaces(req.decimalPlaces());
        }
        if (req.isActive() != null) {
            // Refuse to deactivate the default currency — promote another first.
            if (c.isDefault() && !req.isActive()) {
                throw defaultLocked("Cannot deactivate the default currency", c);
            }
            c.setActive(req.isActive());
        }
        Currency saved = repository.save(c);
        auditService.record(ENTITY, saved.getPublicId(), AuditAction.UPDATE, before, snapshot(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(String publicId) {
        Currency c = load(publicId);
        // Refuse on the default currency or any in-use currency.
        if (c.isDefault()) {
            throw defaultLocked("Cannot delete the default currency", c);
        }
        if (referenceGuard.isInUse(c.getCode())) {
            throw new DomainException(ErrorCode.REFERENCED,
                "Currency '%s' is still referenced and cannot be deleted".formatted(c.getCode()),
                Map.of("currencyCode", c.getCode()));
        }
        Map<String, Object> before = snapshot(c);
        c.softDelete();
        repository.save(c);
        auditService.record(ENTITY, c.getPublicId(), AuditAction.DELETE, before, null);
    }

    /**
     * Promote one currency to default and demote the previous one in the same
     * transaction so the partial unique index never sees a conflicting state.
     * Idempotent when the target is already default. Locked once any
     * transactional history exists — swapping the base currency would silently
     * re-interpret historical amounts.
     */
    @Transactional
    public CurrencyResponse setDefault(String publicId) {
        Currency target = load(publicId);
        if (target.isDefault()) {
            return mapper.toResponse(target);
        }
        if (referenceGuard.hasTransactionalHistory()) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Default currency is locked: transactional history exists",
                Map.of("currencyId", target.getPublicId(), "currencyCode", target.getCode()));
        }
        Map<String, Object> before = snapshot(target);

        // Demote the current default first to keep the partial unique index
        // happy mid-transaction.
        repository.findByIsDefaultTrue().ifPresent(current -> {
            if (!current.getId().equals(target.getId())) {
                current.setDefault(false);
                repository.saveAndFlush(current);
            }
        });

        target.setDefault(true);
        target.setActive(true); // default must be active
        Currency saved = repository.save(target);
        auditService.record(ENTITY, saved.getPublicId(), AuditAction.UPDATE, before, snapshot(saved));
        return mapper.toResponse(saved);
    }

    // --- helpers ---

    private Currency load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("Currency", publicId));
    }

    private void assertVersion(Currency c, Long expected) {
        if (expected != null && expected != c.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK,
                "Currency was modified concurrently",
                Map.of("expected", expected, "actual", c.getVersion()));
        }
    }

    private DomainException defaultLocked(String detail, Currency c) {
        return new DomainException(ErrorCode.CONFLICT, detail,
            Map.of("currencyId", c.getPublicId(), "currencyCode", c.getCode()));
    }

    private Map<String, Object> snapshot(Currency c) {
        return Map.ofEntries(
            Map.entry("id", c.getPublicId()),
            Map.entry("code", c.getCode()),
            Map.entry("name", c.getName()),
            Map.entry("shortName", c.getShortName()),
            Map.entry("country", c.getCountry()),
            Map.entry("symbol", c.getSymbol()),
            Map.entry("decimalPlaces", c.getDecimalPlaces()),
            Map.entry("isDefault", c.isDefault()),
            Map.entry("isActive", c.isActive()),
            Map.entry("version", c.getVersion()));
    }
}

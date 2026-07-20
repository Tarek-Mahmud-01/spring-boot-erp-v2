package com.springboot.erp.modules.procurement.suppliers.service;

import com.springboot.erp.modules.procurement.suppliers.domain.Supplier;
import com.springboot.erp.modules.procurement.suppliers.domain.SupplierStatus;
import com.springboot.erp.modules.procurement.suppliers.domain.SupplierType;
import com.springboot.erp.modules.procurement.suppliers.dto.SupplierDtos.SupplierCreateRequest;
import com.springboot.erp.modules.procurement.suppliers.dto.SupplierDtos.SupplierResponse;
import com.springboot.erp.modules.procurement.suppliers.dto.SupplierDtos.SupplierStatusRequest;
import com.springboot.erp.modules.procurement.suppliers.dto.SupplierDtos.SupplierUpdateRequest;
import com.springboot.erp.modules.procurement.suppliers.mapper.SupplierMapper;
import com.springboot.erp.modules.procurement.suppliers.repository.SupplierRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.money.Money;
import com.springboot.erp.platform.outbox.OutboxPublisher;
import com.springboot.erp.platform.status.StateMachine;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side use-cases for ENT-026 Supplier (reference {@code app.procurement.views.suppliers},
 * US-016 / FR-075–079): create (auto-code, ACTIVE), patch, status transition, and soft-delete.
 * One audit row per mutation; every mutation runs inside a {@code @Transactional} method.
 *
 * <p>The status lifecycle (ACTIVE ⇄ INACTIVE ⇄ BLOCKED) is enforced by the platform
 * {@link StateMachine}. A move to BLOCKED requires a block reason (reference AC-016-3). On create,
 * a non-zero opening balance emits an outbox event so finance can post the balanced opening-balance
 * journal (reference {@code _post_supplier_opening_balance_journal}) without a hard cross-module call.
 */
@Service
public class SupplierCommandService {

    static final String AUDIT_ENTITY = "supplier";
    private static final String AGGREGATE = "supplier";
    private static final String EVENT_OPENING_BALANCE = "procurement.supplier.opening_balance_posted";
    private static final String DEFAULT_CURRENCY = "USD";

    /** ACTIVE ⇄ INACTIVE ⇄ BLOCKED — any status may move to any other (reference has no gate). */
    private static final StateMachine<SupplierStatus> WORKFLOW = StateMachine.builder(SupplierStatus.class)
        .allow(SupplierStatus.ACTIVE, SupplierStatus.INACTIVE, SupplierStatus.BLOCKED)
        .allow(SupplierStatus.INACTIVE, SupplierStatus.ACTIVE, SupplierStatus.BLOCKED)
        .allow(SupplierStatus.BLOCKED, SupplierStatus.ACTIVE, SupplierStatus.INACTIVE)
        .build();

    private final SupplierRepository repository;
    private final SupplierMapper mapper;
    private final AuditService auditService;
    private final OutboxPublisher outbox;

    public SupplierCommandService(SupplierRepository repository, SupplierMapper mapper,
                                  AuditService auditService, OutboxPublisher outbox) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.outbox = outbox;
    }

    /** FR-075 / FR-076 / AC-016-1 — create a Supplier in ACTIVE state with an auto-generated code. */
    @Transactional
    public SupplierResponse create(SupplierCreateRequest req) {
        String code = nextCode();
        if (repository.existsByCode(code)) {
            throw new DomainException(ErrorCode.DUPLICATE, "Supplier code already exists: " + code,
                Map.of("code", code));
        }

        Supplier s = new Supplier();
        s.setCode(code);
        s.setName(req.name().strip());
        s.setType(req.type() == null ? SupplierType.BOTH : req.type());
        s.setLocationId(req.locationId());
        s.setContact(req.contact());
        s.setAddress(req.address());
        s.setPaymentTerms(req.paymentTerms());
        s.setDefaultCurrency(currencyOrDefault(req.defaultCurrency()));
        s.setTaxRegistrationNo(req.taxRegistrationNo());
        s.setAbn(blankToNull(req.abn()));
        s.setBankDetails(req.bankDetails());
        s.setCreditLimit(Money.ofMinor(req.creditLimitAmount(),
            currencyOrDefault(req.creditLimitCurrency())));
        String obCcy = firstNonBlank(req.openingBalanceCurrency(), req.defaultCurrency(), DEFAULT_CURRENCY);
        s.setOpeningBalance(Money.ofMinor(req.openingBalanceAmount(), obCcy.toUpperCase()));
        s.setOpeningBalanceSide(req.openingBalanceSide() == null ? "CREDIT" : req.openingBalanceSide());
        s.setOpeningBalanceDate(req.openingBalanceDate());
        s.setOpeningBalanceExchangeRate(req.openingBalanceExchangeRate());
        s.setOpeningBalanceAccountId(req.openingBalanceAccountId());
        s.setStatus(SupplierStatus.ACTIVE);

        assertOpeningBalanceRate(s);

        Supplier saved = repository.save(s);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null,
            mapper.toResponse(saved));

        // Opening balance → finance posts a balanced journal (reference _post_supplier_opening_balance_journal).
        if (saved.getOpeningBalance().amountMinor() > 0) {
            outbox.publish(AGGREGATE, saved.getPublicId(), EVENT_OPENING_BALANCE, Map.of(
                "supplierId", saved.getPublicId(),
                "code", saved.getCode(),
                "amountMinor", saved.getOpeningBalance().amountMinor(),
                "currency", saved.getOpeningBalance().currency(),
                "side", saved.getOpeningBalanceSide(),
                "accountId", saved.getOpeningBalanceAccountId() == null ? "" : saved.getOpeningBalanceAccountId(),
                "exchangeRate", saved.getOpeningBalanceExchangeRate() == null
                    ? "1" : saved.getOpeningBalanceExchangeRate().toPlainString()));
        }
        return mapper.toResponse(saved);
    }

    /** FR-075 / AC-016-4 — patch supplier fields, bump version via @Version, audit. */
    @Transactional
    public SupplierResponse update(String publicId, SupplierUpdateRequest req) {
        Supplier s = load(publicId);
        checkVersion(s, req.version());
        SupplierResponse before = mapper.toResponse(s);

        SupplierPatcher.apply(s, req);
        assertOpeningBalanceRate(s);

        Supplier saved = repository.save(s);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    /** FR-078 / AC-016-2 / AC-016-3 — flip status through the workflow; block needs a reason. */
    @Transactional
    public SupplierResponse setStatus(String publicId, SupplierStatusRequest req) {
        Supplier s = load(publicId);
        SupplierStatus target = SupplierStatus.fromWire(req.status().strip());

        if (target == SupplierStatus.BLOCKED
            && (req.blockReason() == null || req.blockReason().strip().isEmpty())) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Block reason is required when blocking a supplier.");
        }
        if (s.getStatus() == target) {
            // No-op transition — keep the audit trail meaningful (reference returns current state).
            return mapper.toResponse(s);
        }

        SupplierResponse before = mapper.toResponse(s);
        s.setStatus(WORKFLOW.transition(s.getStatus(), target));
        s.setBlockReason(target == SupplierStatus.BLOCKED ? req.blockReason().strip() : null);

        Supplier saved = repository.save(s);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    /**
     * Soft-delete a supplier — never row-delete (reference keeps FK targets for downstream POs /
     * bills / returns). Flips status to INACTIVE and stamps {@code deleted_at}. Cross-slice
     * transactional-history guards (has-POs / has-bills / …) live in those slices; a supplier with
     * open documents should be deactivated via the status endpoint (deferred until those slices land).
     */
    @Transactional
    public void delete(String publicId) {
        Supplier s = load(publicId);
        SupplierResponse before = mapper.toResponse(s);
        s.setStatus(SupplierStatus.INACTIVE);
        s.softDelete();
        repository.save(s);
        auditService.record(AUDIT_ENTITY, publicId, AuditAction.DELETE, before, null);
    }

    // --- internals -----------------------------------------------------------

    private void assertOpeningBalanceRate(Supplier s) {
        // Reference _assert_opening_balance_rate: a non-base-currency OB must carry a rate, else the
        // GL conversion silently treats it as 1:1. Base currency is company-configured; without that
        // slice wired we require a rate whenever the OB currency differs from the supplier default.
        if (s.getOpeningBalance().amountMinor() <= 0) {
            return;
        }
        String obCcy = s.getOpeningBalance().currency();
        String base = s.getDefaultCurrency();
        if (!obCcy.equalsIgnoreCase(base) && s.getOpeningBalanceExchangeRate() == null) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "An exchange rate is required for an opening balance in " + obCcy
                    + " (supplier default currency is " + base + ").",
                Map.of("currency", obCcy, "baseCurrency", base));
        }
    }

    private String nextCode() {
        return String.format("SUP-%05d", repository.count() + 1);
    }

    private Supplier load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("Supplier", publicId));
    }

    private void checkVersion(Supplier s, Long requestVersion) {
        if (requestVersion != null && requestVersion != s.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK,
                ErrorCode.OPTIMISTIC_LOCK.defaultDetail());
        }
    }

    private static String currencyOrDefault(String v) {
        return v == null || v.isBlank() ? DEFAULT_CURRENCY : v.toUpperCase();
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return DEFAULT_CURRENCY;
    }

    private static String blankToNull(String v) {
        return v == null || v.strip().isEmpty() ? null : v.strip();
    }
}

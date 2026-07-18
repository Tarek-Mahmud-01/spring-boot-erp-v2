package com.guru.erp.modules.procurement.landed.service;

import com.guru.erp.modules.procurement.landed.domain.AllocationBasis;
import com.guru.erp.modules.procurement.landed.domain.LandedCost;
import com.guru.erp.modules.procurement.landed.domain.LandedCostAllocation;
import com.guru.erp.modules.procurement.landed.domain.LandedCostGrnLink;
import com.guru.erp.modules.procurement.landed.domain.LandedCostPoLink;
import com.guru.erp.modules.procurement.landed.domain.LandedCostStatus;
import com.guru.erp.modules.procurement.landed.dto.LandedCostDtos.LandedCostChargeLineRequest;
import com.guru.erp.modules.procurement.landed.dto.LandedCostDtos.LandedCostCreateRequest;
import com.guru.erp.modules.procurement.landed.dto.LandedCostDtos.LandedCostResponse;
import com.guru.erp.modules.procurement.landed.dto.LandedCostDtos.LandedCostUpdateRequest;
import com.guru.erp.modules.procurement.landed.mapper.LandedMapper;
import com.guru.erp.modules.procurement.landed.repository.LandedCostGrnLinkRepository;
import com.guru.erp.modules.procurement.landed.repository.LandedCostPoLinkRepository;
import com.guru.erp.modules.procurement.landed.repository.LandedCostRepository;
import com.guru.erp.platform.audit.AuditAction;
import com.guru.erp.platform.audit.AuditService;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import com.guru.erp.platform.money.Money;
import com.guru.erp.platform.status.StateMachine;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side use-cases for ENT-032 LandedCost (US-019 / FR-104–108). Ports the reference
 * create / update / apply / delete flows: a multi-charge invoice targeting a GRN set OR a PO set,
 * spread across the target lines by an {@link AllocationBasis} into {@link LandedCostAllocation}
 * rows (money math in {@link LandedCostAllocator}), with the full linked set captured in the
 * GRN / PO bridge tables. Applying capitalises the cost into stock — an outbox revaluation event
 * (see {@link LandedCostPostingService}), never a hard call into inventory.
 *
 * <p>Cross-slice line prices/weights are not reachable from this slice, so weights are driven by the
 * per-line qty supplied in {@code lineQtyOverrides}; VALUE therefore weights by qty here (documented
 * divergence from the reference which reads the invoiced bill value). The rounding-exact
 * last-line-remainder split is preserved unchanged.
 */
@Service
public class LandedCostCommandService {

    static final String AUDIT_ENTITY = "landed_cost";
    private static final String DEFAULT_CURRENCY = "USD";

    /** Recorded → applied (capitalised) → reversed (on delete of an applied cost). */
    private static final StateMachine<LandedCostStatus> WORKFLOW = StateMachine.builder(LandedCostStatus.class)
        .allow(LandedCostStatus.DRAFT, LandedCostStatus.APPLIED)
        .allow(LandedCostStatus.APPLIED, LandedCostStatus.REVERSED)
        .build();

    private final LandedCostRepository repository;
    private final LandedCostGrnLinkRepository grnLinks;
    private final LandedCostPoLinkRepository poLinks;
    private final LandedMapper mapper;
    private final AuditService auditService;
    private final LandedCostPostingService posting;
    private final LandedCostNumbering numbering;
    private final Clock clock = Clock.systemUTC();

    public LandedCostCommandService(LandedCostRepository repository,
                                    LandedCostGrnLinkRepository grnLinks,
                                    LandedCostPoLinkRepository poLinks,
                                    LandedMapper mapper, AuditService auditService,
                                    LandedCostPostingService posting, LandedCostNumbering numbering) {
        this.repository = repository;
        this.grnLinks = grnLinks;
        this.poLinks = poLinks;
        this.mapper = mapper;
        this.auditService = auditService;
        this.posting = posting;
        this.numbering = numbering;
    }

    /**
     * FR-104 / FR-105 / FR-106 — record a landed-cost invoice: one row per charge line, sharing a
     * fresh {@code invoiceNumber}, each spread across the target lines. Returns the first charge row.
     */
    @Transactional
    public LandedCostResponse create(LandedCostCreateRequest req) {
        List<String> grnIds = nonNull(req.grnIds());
        List<String> poIds = nonNull(req.poIds());
        if (!grnIds.isEmpty() && !poIds.isEmpty()) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "A landed cost targets a GRN set OR a PO set, not both");
        }
        if (grnIds.isEmpty() && poIds.isEmpty()) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "A landed cost must target at least one GRN or PO");
        }
        rejectUnsupportedBasis(req.allocationBasis());

        String currency = normaliseCurrency(req.currency());
        BigDecimal rate = BigDecimal.ONE; // FX resolution is a finance concern; base == charge ccy here.
        boolean grnBased = !grnIds.isEmpty();

        // Target lines come from the per-line qty override map (line ULID → qty). This is the
        // allocation input the operator entered; weights derive from it.
        Map<String, BigDecimal> overrides = req.lineQtyOverrides() == null
            ? Map.of() : req.lineQtyOverrides();
        if (overrides.isEmpty()) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "lineQtyOverrides must name at least one target line and its allocated qty");
        }
        List<Map.Entry<String, BigDecimal>> targets = new ArrayList<>(
            new LinkedHashMap<>(overrides).entrySet());

        String invoiceNumber = numbering.nextInvoiceNumber();
        List<LandedCost> created = new ArrayList<>();

        for (LandedCostChargeLineRequest chargeLine : req.lines()) {
            long amountMinor = chargeLine.amount();
            long baseMinor = BigDecimal.valueOf(amountMinor).multiply(rate)
                .setScale(0, RoundingMode.HALF_EVEN).longValueExact();

            LandedCost lc = new LandedCost();
            lc.setGrnId(grnBased ? grnIds.get(0) : null);
            lc.setPoId(grnBased ? null : poIds.get(0));
            lc.setInvoiceNumber(invoiceNumber);
            lc.setChargeType(chargeLine.chargeType());
            lc.setSupplierId(req.supplierId());
            lc.setAmount(Money.ofMinor(amountMinor, currency));
            lc.setExchangeRate(rate);
            lc.setBaseAmount(Money.ofMinor(baseMinor, currency));
            lc.setAllocationBasis(req.allocationBasis());
            lc.setStatus(LandedCostStatus.DRAFT);
            lc.setAllocatedAt(req.allocatedAt());

            List<LandedCostAllocator.Line> allocLines = targets.stream()
                .map(e -> LandedCostAllocator.Line.of(e.getValue(), 0L))
                .toList();
            List<Long> slices = LandedCostAllocator.allocate(req.allocationBasis(), baseMinor, allocLines);
            for (int i = 0; i < targets.size(); i++) {
                Map.Entry<String, BigDecimal> t = targets.get(i);
                LandedCostAllocation alloc = new LandedCostAllocation();
                if (grnBased) {
                    alloc.setGrnLineId(t.getKey());
                } else {
                    alloc.setPoLineId(t.getKey());
                }
                alloc.setAllocatedAmount(Money.ofMinor(slices.get(i), currency));
                alloc.setAllocQty(t.getValue());
                lc.addAllocation(alloc);
            }

            LandedCost saved = repository.save(lc);
            // M:N bridge — one row per GRN/PO this charge touches.
            if (grnBased) {
                for (String gid : grnIds) {
                    grnLinks.save(new LandedCostGrnLink(saved.getPublicId(), gid));
                }
            } else {
                for (String pid : poIds) {
                    poLinks.save(new LandedCostPoLink(saved.getPublicId(), pid));
                }
            }
            auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null,
                mapper.toResponse(saved));
            created.add(saved);
        }
        return mapper.toResponse(created.get(0));
    }

    /**
     * FR-108 — apply a recorded landed cost: DRAFT → APPLIED, emit the revaluation/capitalisation
     * outbox event. Applied costs capitalise into stock moving-average cost (handled by the
     * inventory + finance outbox consumers).
     */
    @Transactional
    public LandedCostResponse apply(String publicId) {
        LandedCost lc = load(publicId);
        lc.setStatus(WORKFLOW.transition(lc.getStatus(), LandedCostStatus.APPLIED));
        lc.setAppliedAt(Instant.now(clock));
        repository.save(lc);
        auditService.record(AUDIT_ENTITY, lc.getPublicId(), AuditAction.UPDATE, null,
            mapper.toResponse(lc));
        posting.emitApplied(lc);
        return mapper.toResponse(lc);
    }

    /**
     * Edit a landed cost's non-financial fields. Changing amount / currency re-runs the split;
     * changing the basis is rejected (reference LANDED_COST_BASIS_IMMUTABLE — delete & recreate).
     */
    @Transactional
    public LandedCostResponse update(String publicId, LandedCostUpdateRequest req) {
        LandedCost lc = load(publicId);
        checkVersion(lc, req.version());
        LandedCostResponse before = mapper.toResponse(lc);

        if (req.chargeType() != null) {
            lc.setChargeType(req.chargeType());
        }
        if (req.supplierId() != null) {
            lc.setSupplierId(req.supplierId());
        }
        if (req.grnId() != null) {
            lc.setGrnId(req.grnId());
        }
        if (req.allocatedAt() != null) {
            lc.setAllocatedAt(req.allocatedAt());
        }
        if (req.allocationBasis() != null && req.allocationBasis() != lc.getAllocationBasis()) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Allocation basis is immutable; delete and recreate to change it");
        }
        boolean currencyChanged = req.currency() != null
            && !normaliseCurrency(req.currency()).equals(lc.getAmount().currency());
        boolean amountChanged = req.amount() != null && req.amount() != lc.getAmount().amountMinor();
        if (amountChanged || currencyChanged) {
            reallocate(lc,
                amountChanged ? req.amount() : lc.getAmount().amountMinor(),
                currencyChanged ? normaliseCurrency(req.currency()) : lc.getAmount().currency());
        }

        repository.save(lc);
        auditService.record(AUDIT_ENTITY, lc.getPublicId(), AuditAction.UPDATE, before,
            mapper.toResponse(lc));
        return mapper.toResponse(lc);
    }

    /** Soft-delete a landed cost; an APPLIED one is reversed (revaluation unwind event) first. */
    @Transactional
    public void delete(String publicId) {
        LandedCost lc = load(publicId);
        LandedCostResponse before = mapper.toResponse(lc);
        if (lc.getStatus() == LandedCostStatus.APPLIED) {
            lc.setStatus(WORKFLOW.transition(lc.getStatus(), LandedCostStatus.REVERSED));
            posting.emitReversed(lc);
        }
        grnLinks.deleteByLandedCostId(lc.getPublicId());
        poLinks.deleteByLandedCostId(lc.getPublicId());
        lc.softDelete();
        repository.save(lc);
        auditService.record(AUDIT_ENTITY, publicId, AuditAction.DELETE, before, null);
    }

    // --- internals -----------------------------------------------------------

    /** Recompute the allocation slices + FX for a new amount / currency (reference reallocate). */
    private void reallocate(LandedCost lc, long newAmountMinor, String newCurrency) {
        BigDecimal rate = BigDecimal.ONE;
        long baseMinor = BigDecimal.valueOf(newAmountMinor).multiply(rate)
            .setScale(0, RoundingMode.HALF_EVEN).longValueExact();

        List<LandedCostAllocation> existing = new ArrayList<>(lc.getAllocations());
        List<LandedCostAllocator.Line> allocLines = existing.stream()
            .map(a -> LandedCostAllocator.Line.of(a.getAllocQty(), 0L))
            .toList();
        List<Long> slices = LandedCostAllocator.allocate(lc.getAllocationBasis(), baseMinor, allocLines);
        for (int i = 0; i < existing.size(); i++) {
            existing.get(i).setAllocatedAmount(Money.ofMinor(slices.get(i), newCurrency));
        }
        lc.setAmount(Money.ofMinor(newAmountMinor, newCurrency));
        lc.setExchangeRate(rate);
        lc.setBaseAmount(Money.ofMinor(baseMinor, newCurrency));
    }

    private void rejectUnsupportedBasis(AllocationBasis basis) {
        if (basis == AllocationBasis.VOLUME) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "VOLUME allocation basis is not supported");
        }
    }

    private static String normaliseCurrency(String currency) {
        return currency == null || currency.isBlank()
            ? DEFAULT_CURRENCY : currency.trim().toUpperCase();
    }

    private static List<String> nonNull(List<String> in) {
        return in == null ? List.of() : in;
    }

    private LandedCost load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("LandedCost", publicId));
    }

    private void checkVersion(LandedCost lc, Long requestVersion) {
        if (requestVersion != null && requestVersion != lc.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK,
                ErrorCode.OPTIMISTIC_LOCK.defaultDetail());
        }
    }
}

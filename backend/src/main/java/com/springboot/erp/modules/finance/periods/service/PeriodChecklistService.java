package com.springboot.erp.modules.finance.periods.service;

import com.springboot.erp.modules.finance.gl.domain.JournalEntry;
import com.springboot.erp.modules.finance.periods.domain.ChecklistCheckStatus;
import com.springboot.erp.modules.finance.periods.domain.ChecklistItemKey;
import com.springboot.erp.modules.finance.periods.domain.FiscalPeriod;
import com.springboot.erp.modules.finance.periods.domain.PeriodChecklistItem;
import com.springboot.erp.modules.finance.periods.dto.PeriodChecklistDtos.PeriodChecklistItemResponse;
import com.springboot.erp.modules.finance.periods.mapper.PeriodChecklistMapper;
import com.springboot.erp.modules.finance.periods.repository.GlPeriodQueryRepository;
import com.springboot.erp.modules.finance.periods.repository.PeriodChecklistItemRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.security.CurrentUser;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-249 period-close checklist (reference {@code app.finance.views.period_close.PeriodCloseView}).
 * Every item's automated check re-runs against live data on {@link #runChecks} and again at both
 * gates ({@link #assertChecklistComplete}, {@link #assertDataCleanForClose}) so a stale PASSED
 * can never slip a period through. A period ships all six items as {@code required = true} (strict
 * mode, reference {@code DEFAULT_CHECKLIST}); no advisory items exist at MVP scope.
 *
 * <p>Two evaluators here are GL-native (TRIAL_BALANCE_BALANCED, NO_DRAFT_JOURNALS) and query
 * {@link GlPeriodQueryRepository} directly — {@code gl} and {@code periods} are sub-slices of the
 * same {@code finance} module, so this is a same-module repository call, not a cross-module
 * OutboxPublisher case. The other three reference evaluators (NO_DRAFT_BILLS / NO_DRAFT_POS /
 * NO_NEGATIVE_STOCK / POS_GL_RECONCILED) read procurement/inventory/pos data that belongs to other
 * MODULES not yet ported in this slice's scope; they are registered here as no-op PASS placeholders
 * with a documented detail flag so the checklist row shape stays reference-faithful and a future
 * integration only needs to swap the evaluator function, not the entity/DTO shape.
 */
@Service
public class PeriodChecklistService {

    private static final String AUDIT_ENTITY = "period_checklist_item";

    private final PeriodChecklistItemRepository repository;
    private final GlPeriodQueryRepository glRepository;
    private final PeriodChecklistMapper mapper;
    private final AuditService auditService;
    private final CurrentUser currentUser;

    private final Map<ChecklistItemKey, BiFunction<FiscalPeriod, GlPeriodQueryRepository, EvalResult>> evaluators =
        new EnumMap<>(ChecklistItemKey.class);

    public PeriodChecklistService(PeriodChecklistItemRepository repository, GlPeriodQueryRepository glRepository,
                                  PeriodChecklistMapper mapper, AuditService auditService, CurrentUser currentUser) {
        this.repository = repository;
        this.glRepository = glRepository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.currentUser = currentUser;

        evaluators.put(ChecklistItemKey.TRIAL_BALANCE_BALANCED, this::evalTrialBalance);
        evaluators.put(ChecklistItemKey.NO_DRAFT_JOURNALS, this::evalNoDraftJournals);
        // Out-of-slice evaluators (procurement/inventory/pos modules not owned here) — documented
        // pass-through placeholders, see class javadoc.
        evaluators.put(ChecklistItemKey.NO_DRAFT_BILLS, (p, gl) -> notApplicable("procurement module not in this slice"));
        evaluators.put(ChecklistItemKey.NO_DRAFT_POS, (p, gl) -> notApplicable("pos module not in this slice"));
        evaluators.put(ChecklistItemKey.NO_NEGATIVE_STOCK, (p, gl) -> notApplicable("inventory module not in this slice"));
        evaluators.put(ChecklistItemKey.POS_GL_RECONCILED, (p, gl) -> notApplicable("pos module not in this slice"));
    }

    private record EvalResult(boolean passed, Map<String, Object> detail) {
    }

    private EvalResult notApplicable(String reason) {
        return new EvalResult(true, Map.of("skipped", true, "reason", reason));
    }

    private EvalResult evalTrialBalance(FiscalPeriod period, GlPeriodQueryRepository gl) {
        List<Object[]> rows = gl.sumPostedDebitCredit(period.getCompanyId(), period.getDateTo());
        long debit = rows.isEmpty() ? 0L : ((Number) rows.get(0)[0]).longValue();
        long credit = rows.isEmpty() ? 0L : ((Number) rows.get(0)[1]).longValue();
        return new EvalResult(debit == credit, Map.of("totalDebit", debit, "totalCredit", credit));
    }

    private EvalResult evalNoDraftJournals(FiscalPeriod period, GlPeriodQueryRepository gl) {
        List<JournalEntry> drafts = gl.findDraftInRange(period.getCompanyId(), period.getDateFrom(), period.getDateTo());
        return new EvalResult(drafts.isEmpty(), Map.of("count", drafts.size()));
    }

    /** Idempotently create the default checklist rows for a period (reference {@code _ensure_items}). */
    @Transactional
    public List<PeriodChecklistItem> ensureItems(FiscalPeriod period) {
        List<PeriodChecklistItem> existing = repository.findByFiscalPeriodIdOrderByIdAsc(period.getId());
        if (existing.size() == ChecklistItemKey.values().length) {
            return existing;
        }
        List<ChecklistItemKey> present = existing.stream().map(PeriodChecklistItem::getItemKey).toList();
        for (ChecklistItemKey key : ChecklistItemKey.values()) {
            if (!present.contains(key)) {
                PeriodChecklistItem item = new PeriodChecklistItem();
                item.setFiscalPeriod(period);
                item.setItemKey(key);
                item.setRequired(true);
                item.setCheckStatus(ChecklistCheckStatus.PENDING);
                repository.save(item);
            }
        }
        return repository.findByFiscalPeriodIdOrderByIdAsc(period.getId());
    }

    @Transactional
    public List<PeriodChecklistItemResponse> listChecklist(FiscalPeriod period) {
        return ensureItems(period).stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public List<PeriodChecklistItemResponse> runChecks(FiscalPeriod period) {
        List<PeriodChecklistItem> items = ensureItems(period);
        for (PeriodChecklistItem item : items) {
            evaluate(period, item);
        }
        auditService.record(AUDIT_ENTITY, period.getPublicId(), AuditAction.UPDATE, null,
            Map.of("ranChecks", true, "items", items.size()));
        return items.stream().map(mapper::toResponse).toList();
    }

    private void evaluate(FiscalPeriod period, PeriodChecklistItem item) {
        var evaluator = evaluators.get(item.getItemKey());
        if (evaluator == null) {
            return;
        }
        EvalResult result = evaluator.apply(period, glRepository);
        item.setCheckStatus(result.passed() ? ChecklistCheckStatus.PASSED : ChecklistCheckStatus.FAILED);
        item.setCheckDetail(result.detail());
        item.setCheckedAt(Instant.now());
        // A failing re-check invalidates a prior sign-off — must be re-signed once it passes again.
        if (!result.passed()) {
            item.setSignedOffBy(null);
            item.setSignedOffAt(null);
        }
        repository.save(item);
    }

    @Transactional
    public PeriodChecklistItemResponse setOwner(FiscalPeriod period, ChecklistItemKey itemKey, String ownerUserId) {
        PeriodChecklistItem item = getItem(period, itemKey);
        Map<String, Object> before = Map.of("ownerUserId", String.valueOf(item.getOwnerUserId()));
        item.setOwnerUserId(ownerUserId);
        PeriodChecklistItem saved = repository.save(item);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            Map.of("ownerUserId", String.valueOf(ownerUserId)));
        return mapper.toResponse(saved);
    }

    @Transactional
    public PeriodChecklistItemResponse signOff(FiscalPeriod period, ChecklistItemKey itemKey) {
        PeriodChecklistItem item = getItem(period, itemKey);
        if (item.getCheckStatus() != ChecklistCheckStatus.PASSED) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Checklist item '" + itemKey + "' is " + item.getCheckStatus() + ", not PASSED — cannot sign off");
        }
        item.setSignedOffBy(actorId());
        item.setSignedOffAt(Instant.now());
        PeriodChecklistItem saved = repository.save(item);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, null,
            Map.of("signedOffBy", String.valueOf(saved.getSignedOffBy())));
        return mapper.toResponse(saved);
    }

    /** Gate: VALIDATING -&gt; PENDING_APPROVAL. Re-runs checks first (reference {@code assert_checklist_complete}). */
    @Transactional
    public void assertChecklistComplete(FiscalPeriod period) {
        List<PeriodChecklistItem> items = ensureItems(period);
        List<String> incomplete = new ArrayList<>();
        for (PeriodChecklistItem item : items) {
            if (!item.isRequired()) {
                continue;
            }
            evaluate(period, item);
            if (item.getCheckStatus() != ChecklistCheckStatus.PASSED || item.getSignedOffBy() == null) {
                incomplete.add(item.getItemKey().name());
            }
        }
        if (!incomplete.isEmpty()) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Period-close checklist incomplete for '" + period.getPeriodCode() + "': " + incomplete);
        }
    }

    /** Gate: CLOSING -&gt; CLOSED. Hard data re-check only — does not require/clear sign-off. */
    @Transactional
    public void assertDataCleanForClose(FiscalPeriod period) {
        List<PeriodChecklistItem> items = ensureItems(period);
        List<String> failing = new ArrayList<>();
        for (PeriodChecklistItem item : items) {
            if (!item.isRequired()) {
                continue;
            }
            evaluate(period, item);
            if (item.getCheckStatus() != ChecklistCheckStatus.PASSED) {
                failing.add(item.getItemKey().name());
            }
        }
        if (!failing.isEmpty()) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Period-close data checks failed for '" + period.getPeriodCode() + "': " + failing);
        }
    }

    private PeriodChecklistItem getItem(FiscalPeriod period, ChecklistItemKey itemKey) {
        ensureItems(period);
        return repository.findByFiscalPeriodIdAndItemKey(period.getId(), itemKey)
            .orElseThrow(() -> DomainException.notFound("PeriodChecklistItem", itemKey.name()));
    }

    private String actorId() {
        return currentUser.optional().map(p -> p.userPublicId()).orElse(null);
    }
}

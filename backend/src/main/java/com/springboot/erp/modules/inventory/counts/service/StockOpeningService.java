package com.springboot.erp.modules.inventory.counts.service;

import com.springboot.erp.modules.inventory.counts.domain.StockOpening;
import com.springboot.erp.modules.inventory.counts.domain.StockOpeningStatus;
import com.springboot.erp.modules.inventory.counts.dto.StockOpeningDtos.StockOpeningCreateRequest;
import com.springboot.erp.modules.inventory.counts.dto.StockOpeningDtos.StockOpeningResponse;
import com.springboot.erp.modules.inventory.counts.dto.StockOpeningDtos.StockOpeningUpdateRequest;
import com.springboot.erp.modules.inventory.counts.mapper.CountsMapper;
import com.springboot.erp.modules.inventory.counts.repository.StockOpeningRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.outbox.OutboxPublisher;
import com.springboot.erp.platform.security.CurrentUser;
import com.springboot.erp.platform.status.StateMachine;
import com.springboot.erp.platform.web.PageResponse;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ENT-045 StockOpening use-cases (create / update / post / delete / list / get),
 * ported from app.inventory.views.stock_opening.
 *
 * <p>Lifecycle Draft -&gt; Posted enforced by a {@link StateMachine}. One live
 * opening per (company, product, variant, location) — a second is a duplicate
 * (corrections after posting go through Inventory Adjustments). DRAFT rows are
 * editable / deletable; a POSTED row is immutable.
 *
 * <p>On post the reference writes a StockLedger row (movement ADJUSTMENT,
 * qty=+openingQty, source STOCK_OPENING) and a balanced opening JournalEntry
 * (DR inventory asset / CR the counter GL account). Those cross into the ledger
 * and finance slices, so this service publishes an
 * {@code inventory.stock_opening.posted} outbox event carrying the full posting
 * intent instead of taking a hard cross-slice dependency; the resulting
 * JournalEntry ULID is written back to {@code journalEntryId} by the consumer
 * (left null here). See returned notes.
 */
@Service
public class StockOpeningService {

    private static final String AUDIT_ENTITY = "stock_opening";
    private static final String EVENT_CREATED = "inventory.stock_opening.created";
    private static final String EVENT_UPDATED = "inventory.stock_opening.updated";
    private static final String EVENT_POSTED = "inventory.stock_opening.posted";
    private static final String EVENT_DELETED = "inventory.stock_opening.deleted";

    private static final StateMachine<StockOpeningStatus> LIFECYCLE = StateMachine
        .builder(StockOpeningStatus.class)
        .allow(StockOpeningStatus.DRAFT, StockOpeningStatus.POSTED)
        .build();

    private final StockOpeningRepository repository;
    private final CountsMapper mapper;
    private final AuditService auditService;
    private final OutboxPublisher outbox;
    private final CurrentUser currentUser;

    public StockOpeningService(StockOpeningRepository repository, CountsMapper mapper,
                               AuditService auditService, OutboxPublisher outbox,
                               CurrentUser currentUser) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.outbox = outbox;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public PageResponse<StockOpeningResponse> list(String locationId, Pageable pageable) {
        var page = locationId == null
            ? repository.findAllByOrderByCreatedAtDesc(pageable)
            : repository.findByLocationIdOrderByCreatedAtDesc(locationId, pageable);
        return PageResponse.of(page, mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public StockOpeningResponse get(String publicId) {
        return mapper.toResponse(load(publicId));
    }

    @Transactional
    public StockOpeningResponse create(StockOpeningCreateRequest req) {
        String variantId = blankToNull(req.variantId());
        // One live opening per (company, product, variant, location) — Draft or
        // Posted. A soft-deleted opening frees the key (SQLRestriction excludes it).
        if (repository.existsLiveOpening(req.companyId(), req.productId(), variantId, req.locationId())) {
            throw new DomainException(ErrorCode.DUPLICATE,
                "A stock opening already exists for this product / variant / location.");
        }
        StockOpening e = new StockOpening();
        e.setCompanyId(req.companyId());
        e.setProductId(req.productId());
        e.setVariantId(variantId);
        e.setLocationId(req.locationId());
        e.setOpeningQty(req.openingQty());
        e.setUnitCostAmount(req.unitCostAmount());
        e.setUnitCostCurrency(upper(req.unitCostCurrency()));
        e.setGlAccountId(req.glAccountId());
        e.setNotes(blankToNull(req.notes()));
        e.setStatus(StockOpeningStatus.DRAFT);

        StockOpening saved = repository.save(e);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null,
            mapper.toResponse(saved));
        outbox.publish(AUDIT_ENTITY, saved.getPublicId(), EVENT_CREATED, mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public StockOpeningResponse update(String publicId, StockOpeningUpdateRequest req) {
        StockOpening e = load(publicId);
        checkVersion(e, req.version());
        assertDraft(e);
        StockOpeningResponse before = mapper.toResponse(e);

        if (req.openingQty() != null) {
            e.setOpeningQty(req.openingQty());
        }
        if (req.unitCostAmount() != null) {
            e.setUnitCostAmount(req.unitCostAmount());
        }
        if (req.unitCostCurrency() != null) {
            e.setUnitCostCurrency(upper(req.unitCostCurrency()));
        }
        if (req.glAccountId() != null) {
            e.setGlAccountId(req.glAccountId());
        }
        if (req.notes() != null) {
            e.setNotes(blankToNull(req.notes()));
        }
        StockOpening saved = repository.save(e);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            mapper.toResponse(saved));
        outbox.publish(AUDIT_ENTITY, saved.getPublicId(), EVENT_UPDATED, mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public StockOpeningResponse post(String publicId) {
        StockOpening e = load(publicId);
        if (e.getStatus() == StockOpeningStatus.POSTED) {
            // Idempotent — a second post is a no-op with no extra side effects.
            return mapper.toResponse(e);
        }
        StockOpeningResponse before = mapper.toResponse(e);

        // Enforce Draft -> Posted; throws ILLEGAL_STATE_TRANSITION otherwise.
        e.setStatus(LIFECYCLE.transition(e.getStatus(), StockOpeningStatus.POSTED));
        e.setPostedAt(Instant.now());
        e.setPostedBy(currentUser.optional().map(p -> p.userPublicId()).orElse(null));

        // Publish the ledger + journal posting intent for downstream slices.
        // The consumer writes the StockLedger row and the balanced opening
        // JournalEntry (DR inventory asset / CR gl_account), then may stamp the
        // JE ULID back via journal_entry_id.
        long value = mapper.openingTotalValue(e);
        outbox.publish(AUDIT_ENTITY, e.getPublicId(), EVENT_POSTED, Map.of(
            "openingId", e.getPublicId(),
            "companyId", e.getCompanyId(),
            "productId", e.getProductId(),
            "variantId", e.getVariantId() == null ? "" : e.getVariantId(),
            "locationId", e.getLocationId(),
            "glAccountId", e.getGlAccountId(),
            "openingQty", e.getOpeningQty().toPlainString(),
            "unitCostAmount", e.getUnitCostAmount(),
            "unitCostCurrency", e.getUnitCostCurrency(),
            "totalValue", value));

        StockOpening saved = repository.save(e);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(String publicId) {
        StockOpening e = load(publicId);
        assertDraft(e);
        StockOpeningResponse before = mapper.toResponse(e);
        e.softDelete();
        repository.save(e);
        auditService.record(AUDIT_ENTITY, publicId, AuditAction.DELETE, before, null);
        outbox.publish(AUDIT_ENTITY, publicId, EVENT_DELETED, before);
    }

    // --- helpers -----------------------------------------------------------

    private StockOpening load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("StockOpening", publicId));
    }

    private void assertDraft(StockOpening e) {
        if (e.getStatus() == StockOpeningStatus.POSTED) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Stock opening is posted and can no longer be edited or deleted.");
        }
    }

    private void checkVersion(StockOpening e, Long requestVersion) {
        if (requestVersion != null && requestVersion != e.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK,
                ErrorCode.OPTIMISTIC_LOCK.defaultDetail());
        }
    }

    private static String upper(String v) {
        return v == null ? null : v.toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v;
    }
}

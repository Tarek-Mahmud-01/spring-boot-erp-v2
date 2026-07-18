package com.guru.erp.modules.procurement.receipts.service;

import com.guru.erp.modules.procurement.receipts.domain.DiscrepancyType;
import com.guru.erp.modules.procurement.receipts.domain.GoodsReceipt;
import com.guru.erp.modules.procurement.receipts.domain.GoodsReceiptLine;
import com.guru.erp.modules.procurement.receipts.domain.GrnStatus;
import com.guru.erp.modules.procurement.receipts.dto.ReceiptDtos.GrnCreateRequest;
import com.guru.erp.modules.procurement.receipts.dto.ReceiptDtos.GrnLineCreateRequest;
import com.guru.erp.modules.procurement.receipts.dto.ReceiptDtos.GrnLinePatchItem;
import com.guru.erp.modules.procurement.receipts.dto.ReceiptDtos.GrnPatchRequest;
import com.guru.erp.modules.procurement.receipts.dto.ReceiptDtos.GrnResponse;
import com.guru.erp.modules.procurement.receipts.mapper.ReceiptMapper;
import com.guru.erp.modules.procurement.receipts.repository.GoodsReceiptRepository;
import com.guru.erp.platform.audit.AuditAction;
import com.guru.erp.platform.audit.AuditService;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import com.guru.erp.platform.id.Ulid;
import com.guru.erp.platform.money.Money;
import com.guru.erp.platform.security.CurrentUser;
import com.guru.erp.platform.status.StateMachine;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side use-cases for ENT-029 GoodsReceipt (US-018 / FR-094–097). Ports the reference create /
 * update / transition / confirm / delete flows: header+lines aggregate, the
 * DRAFT → APPROVED → PARTIALLY_RECEIVED → RECEIVED workflow via the platform {@link StateMachine},
 * one audit row per mutation, and a RECEIPT stock-posting outbox event on confirm (handled by
 * {@link ReceiptPostingService}). Cross-module effects (stock post, PO rollup) go through the
 * outbox — this slice never hard-calls the inventory or PO slices.
 */
@Service
public class ReceiptCommandService {

    static final String AUDIT_ENTITY = "goods_receipt";
    private static final String DEFAULT_CURRENCY = "USD";

    /** Reference GRN transitions. RECEIVED is reached via {@link #confirm}, not the generic endpoint. */
    private static final StateMachine<GrnStatus> WORKFLOW = StateMachine.builder(GrnStatus.class)
        .allow(GrnStatus.DRAFT, GrnStatus.APPROVED, GrnStatus.RECEIVED)
        .allow(GrnStatus.APPROVED, GrnStatus.PARTIALLY_RECEIVED, GrnStatus.RECEIVED)
        .allow(GrnStatus.PARTIALLY_RECEIVED, GrnStatus.RECEIVED)
        .build();

    private final GoodsReceiptRepository repository;
    private final ReceiptMapper mapper;
    private final AuditService auditService;
    private final ReceiptPostingService posting;
    private final CurrentUser currentUser;
    private final Clock clock = Clock.systemUTC();

    public ReceiptCommandService(GoodsReceiptRepository repository, ReceiptMapper mapper,
                                 AuditService auditService, ReceiptPostingService posting,
                                 CurrentUser currentUser) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.posting = posting;
        this.currentUser = currentUser;
    }

    @Transactional
    public GrnResponse create(GrnCreateRequest req) {
        GoodsReceipt grn = new GoodsReceipt();
        grn.setNumber(generateNumber());
        grn.setPoId(req.poId());
        grn.setLocationId(req.locationId());
        grn.setReceivedAt(req.receivedAt());
        grn.setReceivedBy(actor());
        grn.setStatus(GrnStatus.DRAFT);
        grn.setAutoReceipt(req.autoReceipt());
        grn.setDeliveryNoteNo(blankToNull(req.deliveryNoteNo()));
        grn.setNotes(blankToNull(req.notes()));
        applyLines(grn, req.lines() == null ? List.of() : req.lines());

        GoodsReceipt saved = repository.save(grn);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null,
            mapper.toResponse(saved));

        // FR-097 — one-step receive: create then confirm (posts stock) in a single call.
        if (req.confirm()) {
            confirmInternal(saved);
        }
        return mapper.toResponse(saved);
    }

    /** PATCH — edit header fields and (Draft only) replace line quantities. */
    @Transactional
    public GrnResponse update(String publicId, GrnPatchRequest req) {
        GoodsReceipt grn = load(publicId);
        checkVersion(grn, req.version());
        GrnResponse before = mapper.toResponse(grn);

        if (req.receivedAt() != null) {
            grn.setReceivedAt(req.receivedAt());
        }
        if (req.deliveryNoteNo() != null) {
            grn.setDeliveryNoteNo(blankToNull(req.deliveryNoteNo()));
        }
        if (req.notes() != null) {
            grn.setNotes(blankToNull(req.notes()));
        }
        if (req.lines() != null && !req.lines().isEmpty()) {
            if (grn.getStatus() != GrnStatus.DRAFT) {
                // A received receipt has already posted stock; editing quantities would desync the
                // ledger. The reference handles the confirmed-edit stock delta cross-slice; here we
                // restrict line edits to Draft and defer the confirmed-edit path to the outbox model.
                throw new DomainException(ErrorCode.CONFLICT,
                    "GRN line quantities can only be edited while Draft; this one is "
                        + grn.getStatus().wire());
            }
            applyLinePatch(grn, req.lines());
        }

        GoodsReceipt saved = repository.save(grn);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    /** Generic non-stock transition: Draft→Approved, Approved→Partially Received. */
    @Transactional
    public GrnResponse transition(String publicId, String toStatusWire) {
        GoodsReceipt grn = load(publicId);
        GrnStatus target = GrnStatus.fromWire(toStatusWire.trim());
        if (target == GrnStatus.RECEIVED || target == GrnStatus.CONFIRMED) {
            // Stock-posting transition is routed to /confirm so the outbox event fires.
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Use the confirm endpoint to move a GRN to Received");
        }
        GrnResponse before = mapper.toResponse(grn);
        grn.setStatus(WORKFLOW.transition(grn.getStatus(), target));
        repository.save(grn);
        auditService.record(AUDIT_ENTITY, grn.getPublicId(), AuditAction.UPDATE, before,
            mapper.toResponse(grn));
        return mapper.toResponse(grn);
    }

    /** FR-097 — confirm ("receive"): → RECEIVED, posts stock via the outbox + rolls up to the PO. */
    @Transactional
    public GrnResponse confirm(String publicId) {
        GoodsReceipt grn = load(publicId);
        confirmInternal(grn);
        return mapper.toResponse(grn);
    }

    /** Delete: an unreceived GRN is soft-deleted; a received one is reversed (stock unwound) first. */
    @Transactional
    public void delete(String publicId) {
        GoodsReceipt grn = load(publicId);
        GrnResponse before = mapper.toResponse(grn);
        if (grn.getStatus().isReceived()) {
            posting.emitReversed(grn);
        }
        grn.softDelete();
        repository.save(grn);
        auditService.record(AUDIT_ENTITY, publicId, AuditAction.DELETE, before, null);
    }

    // --- internals -----------------------------------------------------------

    private void confirmInternal(GoodsReceipt grn) {
        if (grn.getStatus().isReceived()) {
            throw new DomainException(ErrorCode.CONFLICT, "This goods receipt is already received");
        }
        GrnResponse before = mapper.toResponse(grn);
        grn.setStatus(WORKFLOW.transition(grn.getStatus(), GrnStatus.RECEIVED));
        grn.setReceivedBy(actor());
        repository.save(grn);
        auditService.record(AUDIT_ENTITY, grn.getPublicId(), AuditAction.UPDATE, before,
            mapper.toResponse(grn));
        // Post stock + PO rollup via the outbox — the inventory slice consumes this event.
        posting.emitReceived(grn);
    }

    private void applyLines(GoodsReceipt grn, List<GrnLineCreateRequest> lines) {
        for (GrnLineCreateRequest in : lines) {
            GoodsReceiptLine line = new GoodsReceiptLine();
            line.setPoLineId(in.poLineId());
            line.setVariantId(in.variantId());
            line.setQtyReceived(nz(in.qtyReceived()));
            line.setQtyDiscrepancy(nz(in.qtyDiscrepancy()));
            line.setDiscrepancyType(in.discrepancyType() == null || in.discrepancyType().isBlank()
                ? null : DiscrepancyType.fromWire(in.discrepancyType().trim()));
            line.setDiscrepancyNote(in.discrepancyNote());
            line.setBatchNo(in.batchNo());
            line.setSerialNo(in.serialNo());
            line.setExpiryDate(in.expiryDate());
            line.setSupplierBarcode(in.supplierBarcode());
            line.setManufactureDate(in.manufactureDate());
            line.setMrp(money(in.mrpAmount(), in.mrpCurrency()));
            line.setSellPrice(money(in.sellPriceAmount(), in.sellPriceCurrency()));
            grn.addLine(line);
        }
    }

    private void applyLinePatch(GoodsReceipt grn, List<GrnLinePatchItem> items) {
        Map<String, GoodsReceiptLine> byId = grn.getLines().stream()
            .collect(java.util.stream.Collectors.toMap(GoodsReceiptLine::getPublicId, l -> l));
        for (GrnLinePatchItem item : items) {
            GoodsReceiptLine line = byId.get(item.id());
            if (line == null) {
                continue;
            }
            line.setQtyReceived(nz(item.qtyReceived()));
            if (item.batchNo() != null) {
                line.setBatchNo(blankToNull(item.batchNo()));
            }
            if (item.expiryDate() != null) {
                line.setExpiryDate(item.expiryDate());
            }
            if (item.supplierBarcode() != null) {
                line.setSupplierBarcode(blankToNull(item.supplierBarcode()));
            }
            if (item.manufactureDate() != null) {
                line.setManufactureDate(item.manufactureDate());
            }
            if (item.mrpAmount() != null) {
                line.setMrp(money(item.mrpAmount(), item.mrpCurrency()));
            }
            if (item.sellPriceAmount() != null) {
                line.setSellPrice(money(item.sellPriceAmount(), item.sellPriceCurrency()));
            }
        }
    }

    private static Money money(Long amount, String currency) {
        if (amount == null) {
            return null;
        }
        String ccy = currency == null || currency.isBlank() ? DEFAULT_CURRENCY : currency.toUpperCase();
        return Money.ofMinor(amount, ccy);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String actor() {
        return currentUser.optional().map(p -> p.userPublicId()).orElse(null);
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.strip();
    }

    private String generateNumber() {
        String number;
        do {
            number = "GRN-" + Ulid.next().substring(16);
        } while (repository.existsByNumber(number));
        return number;
    }

    private GoodsReceipt load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("GoodsReceipt", publicId));
    }

    private void checkVersion(GoodsReceipt grn, Long requestVersion) {
        if (requestVersion != null && requestVersion != grn.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK,
                ErrorCode.OPTIMISTIC_LOCK.defaultDetail());
        }
    }
}

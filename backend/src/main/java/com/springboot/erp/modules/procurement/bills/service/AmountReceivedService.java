package com.springboot.erp.modules.procurement.bills.service;

import com.springboot.erp.modules.procurement.bills.domain.AmountReceived;
import com.springboot.erp.modules.procurement.bills.domain.AmountReceivedStatus;
import com.springboot.erp.modules.procurement.bills.dto.AmountReceivedDtos.AmountReceivedCreateRequest;
import com.springboot.erp.modules.procurement.bills.dto.AmountReceivedDtos.AmountReceivedResponse;
import com.springboot.erp.modules.procurement.bills.dto.AmountReceivedDtos.AmountReceivedUpdateRequest;
import com.springboot.erp.modules.procurement.bills.mapper.BillMapper;
import com.springboot.erp.modules.procurement.bills.repository.AmountReceivedRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.security.CurrentUser;
import com.springboot.erp.platform.status.StateMachine;
import com.springboot.erp.platform.web.PageResponse;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command + query use-cases for ENT-034 AmountReceived (US-021c) — refund / credit-note receipts.
 * Workflow: DRAFT → APPROVED → CONFIRMED → VOIDED (Draft may skip straight to Confirmed/Voided).
 * Confirming posts a V-004 receipt voucher via the outbox (see {@link AmountReceivedPostingService});
 * editing a confirmed receipt or voiding it reverses the live voucher. One audit row per mutation.
 */
@Service
public class AmountReceivedService {

    static final String AUDIT_ENTITY = "amount_received";

    private static final StateMachine<AmountReceivedStatus> WORKFLOW =
        StateMachine.builder(AmountReceivedStatus.class)
            .allow(AmountReceivedStatus.DRAFT, AmountReceivedStatus.APPROVED,
                AmountReceivedStatus.CONFIRMED, AmountReceivedStatus.VOIDED)
            .allow(AmountReceivedStatus.APPROVED, AmountReceivedStatus.CONFIRMED, AmountReceivedStatus.VOIDED)
            .allow(AmountReceivedStatus.CONFIRMED, AmountReceivedStatus.VOIDED)
            .build();

    private final AmountReceivedRepository repository;
    private final BillMapper mapper;
    private final AuditService audit;
    private final AmountReceivedPostingService posting;
    private final CurrentUser currentUser;

    public AmountReceivedService(AmountReceivedRepository repository, BillMapper mapper,
                                 AuditService audit, AmountReceivedPostingService posting,
                                 CurrentUser currentUser) {
        this.repository = repository;
        this.mapper = mapper;
        this.audit = audit;
        this.posting = posting;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public PageResponse<AmountReceivedResponse> list(String supplierId, String status, String search,
                                                     Pageable pageable) {
        AmountReceivedStatus st = status == null || status.isBlank()
            ? null : AmountReceivedStatus.fromWire(status.trim());
        return PageResponse.of(
            repository.search(blankToNull(supplierId), st, blankToNull(search), pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AmountReceivedResponse get(String publicId) {
        return mapper.toResponse(load(publicId));
    }

    @Transactional
    public AmountReceivedResponse create(AmountReceivedCreateRequest req) {
        String ccy = req.amountCurrency().toUpperCase();
        AmountReceived rec = new AmountReceived();
        rec.setNumber(blank(req.number()) ? nextNumber() : req.number());
        rec.setSupplierId(req.supplierId());
        rec.setPurchaseReturnId(blankToNull(req.purchaseReturnId()));
        rec.setPoId(blankToNull(req.poId()));
        rec.setCreditNoteReference(req.creditNoteReference());
        rec.setPaymentMethodId(req.paymentMethodId());
        rec.setPaymentMethodName(req.paymentMethodName());
        rec.setReceivedDate(req.receivedDate());
        rec.setAmountAmount(req.amountAmount());
        rec.setAmountCurrency(ccy);
        rec.setBaseAmount(req.amountAmount()); // FX to base is the finance seam's job — 1:1 here.
        rec.setReferenceNo(req.referenceNo());
        rec.setNotes(req.notes());
        rec.setDiscountType(req.discountType());
        rec.setDiscountValue(req.discountValue());
        rec.setStatus(AmountReceivedStatus.DRAFT);
        rec.setStatusHistory(new ArrayList<>(List.of(event(AmountReceivedStatus.DRAFT.wire(), null))));
        AmountReceived saved = repository.save(rec);
        audit.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null, mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public AmountReceivedResponse update(String publicId, AmountReceivedUpdateRequest req) {
        AmountReceived rec = load(publicId);
        checkVersion(rec, req.version());
        if (rec.getStatus() == AmountReceivedStatus.VOIDED) {
            throw new DomainException(ErrorCode.CONFLICT, "A voided receipt cannot be edited");
        }
        AmountReceivedResponse before = mapper.toResponse(rec);
        boolean wasConfirmed = rec.getStatus() == AmountReceivedStatus.CONFIRMED;
        if (wasConfirmed) {
            posting.emitReversed(rec);
        }
        if (req.supplierId() != null) {
            rec.setSupplierId(req.supplierId());
        }
        if (req.purchaseReturnId() != null) {
            rec.setPurchaseReturnId(req.purchaseReturnId().isBlank() ? null : req.purchaseReturnId());
        }
        if (req.poId() != null) {
            rec.setPoId(req.poId().isBlank() ? null : req.poId());
        }
        if (req.creditNoteReference() != null) {
            rec.setCreditNoteReference(req.creditNoteReference());
        }
        if (req.paymentMethodId() != null) {
            rec.setPaymentMethodId(req.paymentMethodId());
        }
        if (req.paymentMethodName() != null) {
            rec.setPaymentMethodName(req.paymentMethodName());
        }
        if (req.receivedDate() != null) {
            rec.setReceivedDate(req.receivedDate());
        }
        if (req.amountAmount() != null) {
            rec.setAmountAmount(req.amountAmount());
            rec.setBaseAmount(req.amountAmount());
        }
        if (req.amountCurrency() != null) {
            rec.setAmountCurrency(req.amountCurrency().toUpperCase());
        }
        if (req.referenceNo() != null) {
            rec.setReferenceNo(req.referenceNo());
        }
        if (req.notes() != null) {
            rec.setNotes(req.notes());
        }
        if (req.discountType() != null) {
            rec.setDiscountType(req.discountType());
        }
        if (req.discountValue() != null) {
            rec.setDiscountValue(req.discountValue());
        }
        repository.save(rec);
        if (wasConfirmed) {
            posting.emitPosted(rec);
        }
        audit.record(AUDIT_ENTITY, rec.getPublicId(), AuditAction.UPDATE, before, mapper.toResponse(rec));
        return mapper.toResponse(rec);
    }

    @Transactional
    public AmountReceivedResponse transition(String publicId, String toStatusWire, String reason) {
        AmountReceived rec = load(publicId);
        AmountReceivedStatus to = AmountReceivedStatus.fromWire(toStatusWire);
        if (!WORKFLOW.canTransition(rec.getStatus(), to)) {
            throw DomainException.illegalTransition(rec.getStatus().wire(), to.wire());
        }
        AmountReceivedResponse before = mapper.toResponse(rec);
        rec.setStatus(WORKFLOW.transition(rec.getStatus(), to));
        appendEvent(rec, to.wire(), reason);
        repository.save(rec);
        if (to == AmountReceivedStatus.CONFIRMED) {
            posting.emitPosted(rec);
        }
        audit.record(AUDIT_ENTITY, rec.getPublicId(), AuditAction.UPDATE, before, mapper.toResponse(rec));
        return mapper.toResponse(rec);
    }

    /** Delete surfaces on any status; a posted (non-Draft) receipt first reverses its V-004 voucher. */
    @Transactional
    public void delete(String publicId) {
        AmountReceived rec = load(publicId);
        AmountReceivedResponse before = mapper.toResponse(rec);
        if (rec.getStatus() != AmountReceivedStatus.DRAFT) {
            posting.emitReversed(rec);
        }
        rec.softDelete();
        repository.save(rec);
        audit.record(AUDIT_ENTITY, publicId, AuditAction.DELETE, before, null);
    }

    // --- helpers --------------------------------------------------------------

    /** RCP-YYYY-NNNN, per-year sequence scanned from the max existing number (matches the reference). */
    private String nextNumber() {
        String prefix = "RCP-" + Instant.now().atZone(ZoneOffset.UTC).getYear() + "-";
        String last = repository.maxNumberForPrefix(prefix);
        int n = 1;
        if (last != null) {
            try {
                n = Integer.parseInt(last.substring(prefix.length())) + 1;
            } catch (NumberFormatException ignored) {
                n = 1;
            }
        }
        return String.format("%s%04d", prefix, n);
    }

    private Map<String, Object> event(String status, String reason) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("status", status);
        e.put("actor_name", currentUser.optional().map(p -> p.userPublicId()).orElse("system"));
        e.put("timestamp", Instant.now().toString());
        if (reason != null && !reason.isBlank()) {
            e.put("reason", reason);
        }
        return e;
    }

    private void appendEvent(AmountReceived rec, String status, String reason) {
        List<Map<String, Object>> h = new ArrayList<>(
            rec.getStatusHistory() == null ? List.of() : rec.getStatusHistory());
        h.add(event(status, reason));
        rec.setStatusHistory(h);
    }

    private AmountReceived load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("AmountReceived", publicId));
    }

    private void checkVersion(AmountReceived rec, Long version) {
        if (version != null && version != rec.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK, ErrorCode.OPTIMISTIC_LOCK.defaultDetail());
        }
    }

    private static boolean blank(String v) {
        return v == null || v.isBlank();
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}

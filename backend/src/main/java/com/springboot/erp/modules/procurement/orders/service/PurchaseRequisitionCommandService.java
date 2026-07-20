package com.springboot.erp.modules.procurement.orders.service;

import com.springboot.erp.modules.procurement.orders.domain.PrLineStatus;
import com.springboot.erp.modules.procurement.orders.domain.PrStatus;
import com.springboot.erp.modules.procurement.orders.domain.PurchaseRequisition;
import com.springboot.erp.modules.procurement.orders.domain.PurchaseRequisitionLine;
import com.springboot.erp.modules.procurement.orders.dto.PrDtos.PrCreateRequest;
import com.springboot.erp.modules.procurement.orders.dto.PrDtos.PrLineRequest;
import com.springboot.erp.modules.procurement.orders.dto.PrDtos.PrResponse;
import com.springboot.erp.modules.procurement.orders.dto.PrDtos.PrTransitionRequest;
import com.springboot.erp.modules.procurement.orders.dto.PrDtos.PrUpdateRequest;
import com.springboot.erp.modules.procurement.orders.mapper.OrdersMapper;
import com.springboot.erp.modules.procurement.orders.repository.PurchaseRequisitionRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.money.Money;
import com.springboot.erp.platform.security.CurrentUser;
import com.springboot.erp.platform.status.StateMachine;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side use-cases for ENT-027 PurchaseRequisition (US-017 / FR-080–086). Ports the reference
 * create / update / submit / transition / delete flows: header+lines aggregate, the
 * DRAFT → SUBMITTED → UNDER_REVIEW → SENT_TO_SUPPLIER → CONVERTED (or REJECTED) approval workflow
 * via the platform {@link StateMachine}, and one audit row per mutation.
 *
 * <p>PR→PO conversion lives in {@link PurchaseRequisitionConversionService} so it can call the PO
 * command service without a circular bean. Email/PDF rendering is deferred.
 */
@Service
public class PurchaseRequisitionCommandService {

    static final String AUDIT_ENTITY = "purchase_requisition";
    private static final String DEFAULT_CURRENCY = "USD";

    /** Reference PR_TRANSITIONS. */
    static final StateMachine<PrStatus> WORKFLOW = StateMachine.builder(PrStatus.class)
        .allow(PrStatus.DRAFT, PrStatus.SUBMITTED)
        .allow(PrStatus.SUBMITTED, PrStatus.UNDER_REVIEW, PrStatus.REJECTED)
        .allow(PrStatus.UNDER_REVIEW, PrStatus.SENT_TO_SUPPLIER, PrStatus.CONVERTED, PrStatus.REJECTED)
        .allow(PrStatus.SENT_TO_SUPPLIER, PrStatus.CONVERTED, PrStatus.REJECTED)
        .build();

    private final PurchaseRequisitionRepository repository;
    private final OrdersMapper mapper;
    private final AuditService auditService;
    private final CurrentUser currentUser;

    public PurchaseRequisitionCommandService(PurchaseRequisitionRepository repository,
                                             OrdersMapper mapper, AuditService auditService,
                                             CurrentUser currentUser) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    @Transactional
    public PrResponse create(PrCreateRequest req) {
        PurchaseRequisition pr = new PurchaseRequisition();
        pr.setNumber(nextNumber());
        pr.setLocationId(req.locationId());
        pr.setRequesterUserId(actor());
        pr.setSupplierId(req.supplierId());
        pr.setCurrency(normalizeCurrencyOrNull(req.currency()));
        pr.setNeededByDate(req.neededByDate());
        pr.setRequestDate(req.requestDate());
        pr.setPaymentTerms(req.paymentTerms());
        pr.setExchangeRate(req.exchangeRate());
        pr.setInvoiceDiscountType(req.invoiceDiscountType());
        pr.setInvoiceDiscountValue(nvl(req.invoiceDiscountValue()));
        pr.setTotalAmount(req.totalAmount());
        pr.setNotes(req.notes());
        pr.setStatus(PrStatus.DRAFT);
        applyLines(pr, req.lines());

        PurchaseRequisition saved = repository.save(pr);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public PrResponse update(String publicId, PrUpdateRequest req) {
        PurchaseRequisition pr = load(publicId);
        checkVersion(pr, req.version());
        if (pr.getStatus() != PrStatus.DRAFT) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Only a Draft requisition can be edited; this one is " + pr.getStatus().wire());
        }
        PrResponse before = mapper.toResponse(pr);

        pr.setLocationId(req.locationId());
        pr.setSupplierId(req.supplierId());
        pr.setCurrency(normalizeCurrencyOrNull(req.currency()));
        pr.setNeededByDate(req.neededByDate());
        pr.setRequestDate(req.requestDate());
        pr.setPaymentTerms(req.paymentTerms());
        pr.setExchangeRate(req.exchangeRate());
        pr.setInvoiceDiscountType(req.invoiceDiscountType());
        pr.setInvoiceDiscountValue(nvl(req.invoiceDiscountValue()));
        pr.setTotalAmount(req.totalAmount());
        pr.setNotes(req.notes());
        if (req.lines() != null) {
            pr.clearLines();
            applyLines(pr, req.lines());
        }

        PurchaseRequisition saved = repository.save(pr);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    /** FR-081 / FR-083 — submit for approval: DRAFT → SUBMITTED. Requires at least one line. */
    @Transactional
    public PrResponse submit(String publicId) {
        PurchaseRequisition pr = load(publicId);
        if (pr.getLines().isEmpty()) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "A requisition must have at least one line before it can be submitted.");
        }
        PrResponse before = mapper.toResponse(pr);
        pr.setStatus(WORKFLOW.transition(pr.getStatus(), PrStatus.SUBMITTED));
        PurchaseRequisition saved = repository.save(pr);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    /** FR-085 / FR-086 — generic workflow move (approve→under-review, reject, send-to-supplier). */
    @Transactional
    public PrResponse transition(String publicId, PrTransitionRequest req) {
        PurchaseRequisition pr = load(publicId);
        PrStatus to = PrStatus.fromWire(req.toStatus());
        PrResponse before = mapper.toResponse(pr);

        pr.setStatus(WORKFLOW.transition(pr.getStatus(), to));
        if (to == PrStatus.REJECTED) {
            pr.setRejectionReason(req.reason());
        }
        if (req.buyerId() != null) {
            pr.setAssignedBuyerId(req.buyerId());
        }
        PurchaseRequisition saved = repository.save(pr);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    /** Delete a DRAFT requisition (soft delete). Non-draft PRs must be rejected first. */
    @Transactional
    public void delete(String publicId) {
        PurchaseRequisition pr = load(publicId);
        if (pr.getStatus() != PrStatus.DRAFT) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Only a Draft requisition can be deleted; reject this one first.");
        }
        PrResponse before = mapper.toResponse(pr);
        pr.softDelete();
        repository.save(pr);
        auditService.record(AUDIT_ENTITY, publicId, AuditAction.DELETE, before, null);
    }

    // --- internals -----------------------------------------------------------

    private void applyLines(PurchaseRequisition pr, List<PrLineRequest> lines) {
        if (lines == null) {
            return;
        }
        int lineNo = 1;
        for (PrLineRequest in : lines) {
            String ccy = normalizeCurrency(in.unitPriceCurrency());
            PurchaseRequisitionLine line = new PurchaseRequisitionLine();
            line.setLineNo(lineNo++);
            line.setProductId(in.productId());
            line.setVariantId(in.variantId());
            line.setQty(in.qty());
            line.setUomId(in.uomId());
            line.setPreferredSupplierId(in.preferredSupplierId());
            line.setDescription(in.description());
            line.setUnitPrice(Money.ofMinor(in.unitPriceAmount(), ccy));
            line.setDiscountPercent(nvl(in.discountPercent()));
            line.setTaxCodeId(in.taxCodeId());
            // Honour the frontend-precomputed total when present; else derive it (reference parity).
            long total = in.lineTotalAmount() != 0
                ? in.lineTotalAmount()
                : LineMath.lineTotal(in.qty(), in.unitPriceAmount(), nvl(in.discountPercent()));
            line.setLineTotalAmount(total);
            line.setStatus(PrLineStatus.OPEN);
            pr.addLine(line);
        }
    }

    private String actor() {
        return currentUser.optional().map(p -> p.userPublicId()).orElse(null);
    }

    private String nextNumber() {
        long n = repository.countByNumberStartingWith("PR-");
        String number;
        do {
            number = String.format("PR-%05d", ++n);
        } while (repository.existsByNumber(number));
        return number;
    }

    private PurchaseRequisition load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("PurchaseRequisition", publicId));
    }

    private void checkVersion(PurchaseRequisition pr, Long requestVersion) {
        if (requestVersion != null && requestVersion != pr.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK,
                ErrorCode.OPTIMISTIC_LOCK.defaultDetail());
        }
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String normalizeCurrency(String ccy) {
        return ccy == null || ccy.isBlank() ? DEFAULT_CURRENCY : ccy.trim().toUpperCase();
    }

    private static String normalizeCurrencyOrNull(String ccy) {
        return ccy == null || ccy.isBlank() ? null : ccy.trim().toUpperCase();
    }
}

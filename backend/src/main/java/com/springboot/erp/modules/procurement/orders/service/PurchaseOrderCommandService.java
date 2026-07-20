package com.springboot.erp.modules.procurement.orders.service;

import com.springboot.erp.modules.procurement.orders.domain.PoStatus;
import com.springboot.erp.modules.procurement.orders.domain.PurchaseOrder;
import com.springboot.erp.modules.procurement.orders.domain.PurchaseOrderLine;
import com.springboot.erp.modules.procurement.orders.domain.PurchaseOrderVersion;
import com.springboot.erp.modules.procurement.orders.dto.PoDtos.PoCreateRequest;
import com.springboot.erp.modules.procurement.orders.dto.PoDtos.PoLineRequest;
import com.springboot.erp.modules.procurement.orders.dto.PoDtos.PoResponse;
import com.springboot.erp.modules.procurement.orders.dto.PoDtos.PoTransitionRequest;
import com.springboot.erp.modules.procurement.orders.dto.PoDtos.PoUpdateRequest;
import com.springboot.erp.modules.procurement.orders.mapper.OrdersMapper;
import com.springboot.erp.modules.procurement.orders.repository.PurchaseOrderRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.money.Money;
import com.springboot.erp.platform.security.CurrentUser;
import com.springboot.erp.platform.status.StateMachine;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side use-cases for ENT-028 PurchaseOrder (US-018 / FR-087–093). Ports the reference
 * create / amend / transition / delete flows: header+lines aggregate, the
 * DRAFT → SUBMITTED → APPROVED → SENT → RECEIVED → CLOSED workflow (plus the frontend shortcut edges
 * and CANCELLED) via the platform {@link StateMachine}, amendment history in
 * {@link PurchaseOrderVersion} (FR-092), one audit row per mutation, and a status-change outbox
 * event on transitions (see {@link PurchaseOrderPostingService}).
 *
 * <p>The reference "Direct PO" one-shot chain (auto-create GRN + bill + payments) and email/PDF
 * rendering are DEFERRED to downstream slices; the {@code isDirect} flag is persisted and a
 * {@code purchase_order.created} outbox event is emitted so a consumer can complete that chain.
 */
@Service
public class PurchaseOrderCommandService {

    static final String AUDIT_ENTITY = "purchase_order";
    private static final String DEFAULT_CURRENCY = "USD";

    /** Reference PO_TRANSITIONS. */
    static final StateMachine<PoStatus> WORKFLOW = StateMachine.builder(PoStatus.class)
        .allow(PoStatus.DRAFT, PoStatus.SUBMITTED, PoStatus.APPROVED, PoStatus.CANCELLED)
        .allow(PoStatus.SUBMITTED, PoStatus.APPROVED, PoStatus.CANCELLED)
        .allow(PoStatus.APPROVED, PoStatus.SENT, PoStatus.PARTIALLY_RECEIVED, PoStatus.RECEIVED,
            PoStatus.CANCELLED)
        .allow(PoStatus.SENT, PoStatus.PARTIALLY_RECEIVED, PoStatus.RECEIVED, PoStatus.CLOSED)
        .allow(PoStatus.PARTIALLY_RECEIVED, PoStatus.RECEIVED, PoStatus.CLOSED)
        .allow(PoStatus.RECEIVED, PoStatus.CLOSED)
        .build();

    private final PurchaseOrderRepository repository;
    private final OrdersMapper mapper;
    private final AuditService auditService;
    private final PurchaseOrderPostingService posting;
    private final CurrentUser currentUser;

    public PurchaseOrderCommandService(PurchaseOrderRepository repository, OrdersMapper mapper,
                                       AuditService auditService,
                                       PurchaseOrderPostingService posting, CurrentUser currentUser) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.posting = posting;
        this.currentUser = currentUser;
    }

    @Transactional
    public PoResponse create(PoCreateRequest req) {
        // Reference: a Direct PO must carry at least one product line.
        if (req.isDirect() && req.lines().stream().noneMatch(l -> l.productId() != null)) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "A Direct PO must have at least one product line.");
        }
        String currency = normalizeCurrency(req.currency());

        PurchaseOrder po = new PurchaseOrder();
        po.setNumber(nextNumber());
        po.setSupplierId(req.supplierId());
        po.setLocationId(req.locationId());
        po.setPoDate(req.poDate());
        po.setExpectedDeliveryDate(req.expectedDeliveryDate());
        po.setCurrency(currency);
        po.setExchangeRate(req.exchangeRate() == null ? BigDecimal.ONE : req.exchangeRate());
        po.setPaymentTerms(req.paymentTerms());
        po.setSourcePrId(req.sourcePrId());
        po.setNotes(req.notes());
        po.setInvoiceDiscountType(req.invoiceDiscountType());
        po.setInvoiceDiscountValue(nvl(req.invoiceDiscountValue()));
        po.setDirect(req.isDirect());
        po.setStatus(PoStatus.DRAFT);
        po.setPoVersion(1);
        applyLines(po, req.lines(), currency);
        addVersionSnapshot(po, "PO created");

        PurchaseOrder saved = repository.save(po);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null,
            mapper.toResponse(saved));
        posting.emitCreated(saved);
        return mapper.toResponse(saved);
    }

    /** Package-private convenience used by the PR→PO conversion service (returns the entity). */
    @Transactional
    PurchaseOrder createEntity(PoCreateRequest req) {
        PoResponse response = create(req);
        return repository.findByPublicId(response.id()).orElseThrow();
    }

    /** FR-092 — amend a DRAFT PO: full header + line replacement, bumps the amendment version. */
    @Transactional
    public PoResponse update(String publicId, PoUpdateRequest req) {
        PurchaseOrder po = load(publicId);
        checkVersion(po, req.version());
        if (po.getStatus() != PoStatus.DRAFT) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Only a Draft purchase order can be amended; this one is " + po.getStatus().wire());
        }
        PoResponse before = mapper.toResponse(po);

        if (req.expectedDeliveryDate() != null) {
            po.setExpectedDeliveryDate(req.expectedDeliveryDate());
        }
        if (req.paymentTerms() != null) {
            po.setPaymentTerms(req.paymentTerms());
        }
        if (req.notes() != null) {
            po.setNotes(req.notes());
        }
        if (req.invoiceDiscountType() != null) {
            po.setInvoiceDiscountType(req.invoiceDiscountType());
        }
        if (req.invoiceDiscountValue() != null) {
            po.setInvoiceDiscountValue(req.invoiceDiscountValue());
        }
        if (req.supplierId() != null) {
            po.setSupplierId(req.supplierId());
        }
        if (req.locationId() != null) {
            po.setLocationId(req.locationId());
        }
        if (req.currency() != null) {
            po.setCurrency(normalizeCurrency(req.currency()));
        }
        if (req.exchangeRate() != null) {
            po.setExchangeRate(req.exchangeRate());
        }
        if (req.poDate() != null) {
            po.setPoDate(req.poDate());
        }
        if (req.lines() != null) {
            po.clearLines();
            applyLines(po, req.lines(), po.getCurrency());
        }

        po.setPoVersion(po.getPoVersion() + 1);
        addVersionSnapshot(po, req.amendmentReason());

        PurchaseOrder saved = repository.save(po);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    /**
     * FR-090 / FR-093 — generic workflow move (submit / approve / send / receive / close / cancel).
     * Cancellation is blocked once goods have been received (guard delegated to a downstream slice
     * via the posting event; the state-machine still forbids illegal edges).
     */
    @Transactional
    public PoResponse transition(String publicId, PoTransitionRequest req) {
        PurchaseOrder po = load(publicId);
        PoStatus to = PoStatus.fromWire(req.toStatus());
        PoResponse before = mapper.toResponse(po);

        po.setStatus(WORKFLOW.transition(po.getStatus(), to));
        if (to == PoStatus.CLOSED) {
            po.setCloseReason(req.reason());
        }
        PurchaseOrder saved = repository.save(po);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            mapper.toResponse(saved));
        posting.emitStatusChanged(saved, to, req.reason());
        return mapper.toResponse(saved);
    }

    /** Delete a DRAFT/CANCELLED PO (soft delete). A received PO cannot be deleted. */
    @Transactional
    public void delete(String publicId) {
        PurchaseOrder po = load(publicId);
        if (po.getStatus() != PoStatus.DRAFT && po.getStatus() != PoStatus.CANCELLED) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Only a Draft or Cancelled purchase order can be deleted; this one is "
                    + po.getStatus().wire());
        }
        PoResponse before = mapper.toResponse(po);
        po.softDelete();
        repository.save(po);
        auditService.record(AUDIT_ENTITY, publicId, AuditAction.DELETE, before, null);
    }

    // --- internals -----------------------------------------------------------

    private void applyLines(PurchaseOrder po, List<PoLineRequest> lines, String headerCurrency) {
        if (lines == null) {
            return;
        }
        int lineNo = 1;
        for (PoLineRequest in : lines) {
            String ccy = in.unitPriceCurrency() == null || in.unitPriceCurrency().isBlank()
                ? headerCurrency : in.unitPriceCurrency().trim().toUpperCase();
            long total = LineMath.lineTotal(in.qtyOrdered(), in.unitPriceAmount(),
                nvl(in.discountPercent()));
            PurchaseOrderLine line = new PurchaseOrderLine();
            line.setLineNo(lineNo++);
            line.setProductId(in.productId());
            line.setVariantId(in.variantId());
            line.setQtyOrdered(in.qtyOrdered());
            line.setQtyReceivedTotal(BigDecimal.ZERO);
            line.setUomId(in.uomId());
            line.setUnitPrice(Money.ofMinor(in.unitPriceAmount(), ccy));
            line.setDiscountPercent(nvl(in.discountPercent()));
            line.setTaxCodeId(in.taxCodeId());
            line.setLineTotal(Money.ofMinor(total, headerCurrency));
            po.addLine(line);
        }
    }

    /** Reference {@code _snapshot_version}: capture the header snapshot at the current poVersion. */
    private void addVersionSnapshot(PurchaseOrder po, String reason) {
        PurchaseOrderVersion v = new PurchaseOrderVersion();
        v.setVersionNo(po.getPoVersion());
        v.setSnapshot(headerSnapshot(po));
        v.setReason(reason);
        po.addVersion(v);
    }

    /** Reference {@code _snapshot}. */
    private Map<String, Object> headerSnapshot(PurchaseOrder po) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("number", po.getNumber());
        m.put("status", po.getStatus().wire());
        m.put("currency", po.getCurrency());
        m.put("exchange_rate", po.getExchangeRate());
        m.put("payment_terms", po.getPaymentTerms());
        m.put("po_version", po.getPoVersion());
        return m;
    }

    private String nextNumber() {
        long n = repository.countByNumberStartingWith("PO-");
        String number;
        do {
            number = String.format("PO-%05d", ++n);
        } while (repository.existsByNumber(number));
        return number;
    }

    private PurchaseOrder load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("PurchaseOrder", publicId));
    }

    private void checkVersion(PurchaseOrder po, Long requestVersion) {
        if (requestVersion != null && requestVersion != po.getVersion()) {
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
}

package com.springboot.erp.modules.procurement.bills.service;

import com.springboot.erp.modules.procurement.bills.domain.BillStatus;
import com.springboot.erp.modules.procurement.bills.domain.SupplierBill;
import com.springboot.erp.modules.procurement.bills.domain.SupplierPayment;
import com.springboot.erp.modules.procurement.bills.domain.SupplierPaymentStatus;
import com.springboot.erp.modules.procurement.bills.domain.SupplierPaymentTender;
import com.springboot.erp.modules.procurement.bills.dto.PaymentDtos.PaymentCreateRequest;
import com.springboot.erp.modules.procurement.bills.dto.PaymentDtos.PaymentResponse;
import com.springboot.erp.modules.procurement.bills.dto.PaymentDtos.PaymentUpdateRequest;
import com.springboot.erp.modules.procurement.bills.dto.PaymentDtos.TenderRequest;
import com.springboot.erp.modules.procurement.bills.mapper.BillMapper;
import com.springboot.erp.modules.procurement.bills.repository.SupplierBillRepository;
import com.springboot.erp.modules.procurement.bills.repository.SupplierPaymentRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.security.CurrentUser;
import com.springboot.erp.platform.status.StateMachine;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side use-cases for ENT-033 SupplierPayment (US-021): create (with split-payment tenders +
 * P2P overpayment guard), update (Draft only), transition (Draft → Approved → Partially Paid →
 * Paid), and delete (Draft only). Confirming (Approved) posts a V-003 voucher via the outbox (see
 * {@link PaymentPostingService}) and recomputes the PO's bill / sibling-payment paid state.
 */
@Service
public class PaymentCommandService {

    static final String AUDIT_ENTITY = "supplier_payment";

    private static final StateMachine<SupplierPaymentStatus> WORKFLOW =
        StateMachine.builder(SupplierPaymentStatus.class)
            .allow(SupplierPaymentStatus.DRAFT, SupplierPaymentStatus.APPROVED)
            .allow(SupplierPaymentStatus.APPROVED, SupplierPaymentStatus.PARTIALLY_PAID, SupplierPaymentStatus.PAID)
            .allow(SupplierPaymentStatus.PARTIALLY_PAID, SupplierPaymentStatus.PAID)
            .build();

    private final SupplierPaymentRepository repository;
    private final SupplierBillRepository billRepository;
    private final BillMapper mapper;
    private final AuditService audit;
    private final PaymentPostingService posting;
    private final CurrentUser currentUser;

    public PaymentCommandService(SupplierPaymentRepository repository, SupplierBillRepository billRepository,
                                 BillMapper mapper, AuditService audit, PaymentPostingService posting,
                                 CurrentUser currentUser) {
        this.repository = repository;
        this.billRepository = billRepository;
        this.mapper = mapper;
        this.audit = audit;
        this.posting = posting;
        this.currentUser = currentUser;
    }

    @Transactional
    public PaymentResponse create(PaymentCreateRequest req) {
        validateTenders(req.tenders(), req.amountAmount());
        assertPaymentAllowed(req.poId(), req.amountAmount());
        String ccy = req.amountCurrency().toUpperCase();

        SupplierPayment pay = new SupplierPayment();
        pay.setNumber(nextNumber());
        pay.setSupplierId(req.supplierId());
        pay.setPoId(blankToNull(req.poId()));
        pay.setInvoiceReference(req.invoiceReference());
        pay.setPaymentMethodId(req.paymentMethodId());
        pay.setPaymentMethodName(req.paymentMethodName());
        pay.setPaymentDate(req.paymentDate());
        pay.setAmountAmount(req.amountAmount());
        pay.setAmountCurrency(ccy);
        // FX to base currency (rate lookup) is the finance seam's job — default 1:1 here.
        pay.setBaseAmount(req.amountAmount());
        pay.setReferenceNo(req.referenceNo());
        pay.setNotes(req.notes());
        pay.setDiscountType(req.discountType());
        pay.setDiscountValue(req.discountValue());
        pay.setStatus(SupplierPaymentStatus.DRAFT);
        pay.setStatusHistory(new ArrayList<>(List.of(event(SupplierPaymentStatus.DRAFT.wire()))));
        if (req.tenders() != null) {
            for (TenderRequest t : req.tenders()) {
                SupplierPaymentTender tender = new SupplierPaymentTender();
                tender.setPaymentMethodId(t.paymentMethodId());
                tender.setPaymentMethodName(t.paymentMethodName());
                tender.setAmountAmount(t.amountAmount());
                tender.setAmountCurrency(blank(t.amountCurrency()) ? ccy : t.amountCurrency().toUpperCase());
                tender.setReferenceNo(t.referenceNo());
                pay.addTender(tender);
            }
        }
        SupplierPayment saved = repository.save(pay);
        audit.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null, mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public PaymentResponse update(String publicId, PaymentUpdateRequest req) {
        SupplierPayment pay = load(publicId);
        checkVersion(pay, req.version());
        if (pay.getStatus() != SupplierPaymentStatus.DRAFT) {
            throw new DomainException(ErrorCode.CONFLICT, "Only a Draft payment can be edited");
        }
        PaymentResponse before = mapper.toResponse(pay);
        if (req.poId() != null) {
            pay.setPoId(req.poId().isBlank() ? null : req.poId());
        }
        if (req.invoiceReference() != null) {
            pay.setInvoiceReference(req.invoiceReference());
        }
        if (req.paymentMethodId() != null) {
            pay.setPaymentMethodId(req.paymentMethodId());
        }
        if (req.paymentMethodName() != null) {
            pay.setPaymentMethodName(req.paymentMethodName());
        }
        if (req.paymentDate() != null) {
            pay.setPaymentDate(req.paymentDate());
        }
        if (req.amountAmount() != null) {
            pay.setAmountAmount(req.amountAmount());
            pay.setBaseAmount(req.amountAmount());
        }
        if (req.amountCurrency() != null) {
            pay.setAmountCurrency(req.amountCurrency().toUpperCase());
        }
        if (req.referenceNo() != null) {
            pay.setReferenceNo(req.referenceNo());
        }
        if (req.notes() != null) {
            pay.setNotes(req.notes());
        }
        if (req.discountType() != null) {
            pay.setDiscountType(req.discountType());
        }
        if (req.discountValue() != null) {
            pay.setDiscountValue(req.discountValue());
        }
        assertPaymentAllowed(pay.getPoId(), pay.getAmountAmount());
        SupplierPayment saved = repository.save(pay);
        audit.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before, mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public PaymentResponse transition(String publicId, String toStatusWire) {
        SupplierPayment pay = load(publicId);
        SupplierPaymentStatus to = SupplierPaymentStatus.fromWire(toStatusWire);
        if (!WORKFLOW.canTransition(pay.getStatus(), to)) {
            throw DomainException.illegalTransition(pay.getStatus().wire(), to.wire());
        }
        if (to == SupplierPaymentStatus.APPROVED) {
            assertPaymentAllowed(pay.getPoId(), pay.getAmountAmount());
        }
        PaymentResponse before = mapper.toResponse(pay);
        pay.setStatus(WORKFLOW.transition(pay.getStatus(), to));
        appendEvent(pay, to.wire());
        repository.save(pay);
        if (to == SupplierPaymentStatus.APPROVED) {
            posting.emitPosted(pay);
            recomputePaidStateForPo(pay.getPoId());
        }
        audit.record(AUDIT_ENTITY, pay.getPublicId(), AuditAction.UPDATE, before, mapper.toResponse(pay));
        return mapper.toResponse(pay);
    }

    @Transactional
    public void delete(String publicId) {
        SupplierPayment pay = load(publicId);
        if (pay.getStatus() != SupplierPaymentStatus.DRAFT) {
            throw new DomainException(ErrorCode.CONFLICT, "Only a Draft payment can be deleted");
        }
        PaymentResponse before = mapper.toResponse(pay);
        pay.softDelete();
        repository.save(pay);
        audit.record(AUDIT_ENTITY, publicId, AuditAction.DELETE, before, null);
    }

    // --- P2P guard + paid-state recompute -------------------------------------

    /** Require an approved bill, block overpayment, block a fully-paid PO (reference _assert_payment_allowed). */
    private void assertPaymentAllowed(String poId, long amount) {
        if (blank(poId)) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "A supplier payment must reference a purchase order with an approved bill");
        }
        long billed = billRepository.billedTotalForPo(poId);
        if (billed <= 0) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "No approved bill exists for this purchase order");
        }
        long outstanding = billed - repository.confirmedPaidForPo(poId);
        if (outstanding <= 0) {
            throw new DomainException(ErrorCode.CONFLICT, "This purchase order's bills are already fully paid");
        }
        if (amount > outstanding) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Payment " + amount + " exceeds the outstanding " + outstanding);
        }
    }

    /** Re-derive every bill + active payment on the PO from Σ billed vs Σ confirmed paid. */
    private void recomputePaidStateForPo(String poId) {
        if (blank(poId)) {
            return;
        }
        long billed = billRepository.billedTotalForPo(poId);
        long paid = repository.confirmedPaidForPo(poId);
        List<SupplierBill> bills = billRepository.activeBillsForPo(poId);
        if (bills.isEmpty()) {
            return;
        }
        BillStatus billStatus;
        SupplierPaymentStatus paymentStatus;
        if (billed > 0 && paid >= billed) {
            billStatus = BillStatus.PAID;
            paymentStatus = SupplierPaymentStatus.PAID;
        } else if (paid > 0) {
            billStatus = BillStatus.PARTIALLY_PAID;
            paymentStatus = SupplierPaymentStatus.PARTIALLY_PAID;
        } else {
            return;
        }
        for (SupplierBill b : bills) {
            if (b.getStatus() != billStatus) {
                b.setStatus(billStatus);
                billRepository.save(b);
            }
        }
        for (SupplierPayment p : repository.activePaymentsForPo(poId)) {
            if (p.getStatus() != paymentStatus) {
                p.setStatus(paymentStatus);
                repository.save(p);
            }
        }
    }

    // --- helpers --------------------------------------------------------------

    private void validateTenders(List<TenderRequest> tenders, long amount) {
        if (tenders == null || tenders.isEmpty()) {
            return;
        }
        long sum = tenders.stream().mapToLong(TenderRequest::amountAmount).sum();
        if (sum != amount) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Payment tenders total " + sum + " but the payment amount is " + amount + " — they must be equal");
        }
    }

    private Map<String, Object> event(String status) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("status", status);
        e.put("actor_name", currentUser.optional().map(p -> p.userPublicId()).orElse("system"));
        e.put("timestamp", Instant.now().toString());
        return e;
    }

    private void appendEvent(SupplierPayment pay, String status) {
        List<Map<String, Object>> h = new ArrayList<>(
            pay.getStatusHistory() == null ? List.of() : pay.getStatusHistory());
        h.add(event(status));
        pay.setStatusHistory(h);
    }

    /** PAY-NNNNN, count-derived (matches the reference). count() spans soft-deleted rows too, so
     *  the sequence only advances — collisions are not possible. */
    private String nextNumber() {
        return String.format("PAY-%05d", repository.count() + 1);
    }

    private SupplierPayment load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("SupplierPayment", publicId));
    }

    private void checkVersion(SupplierPayment pay, Long version) {
        if (version != null && version != pay.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK, ErrorCode.OPTIMISTIC_LOCK.defaultDetail());
        }
    }

    private static boolean blank(String v) {
        return v == null || v.isBlank();
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v;
    }
}

package com.springboot.erp.modules.pos.transactions.service;

import com.springboot.erp.modules.pos.transactions.domain.PosTender;
import com.springboot.erp.modules.pos.transactions.domain.PosTransaction;
import com.springboot.erp.modules.pos.transactions.domain.PosTransactionStatus;
import com.springboot.erp.modules.pos.transactions.dto.TransactionDtos.PosTransactionResponse;
import com.springboot.erp.modules.pos.transactions.dto.TransactionDtos.TenderRequest;
import com.springboot.erp.modules.pos.transactions.repository.PosTenderRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-033 FR-172..176 — payment legs on an open sale (reference {@code app.pos.views}
 * {@code add_tender} / {@code undo_tender}). Split across its own service (not
 * {@link TransactionCommandService}) because tendering is a distinct concern with its own
 * surcharge/overpayment rules, per the CAPS split-as-needed guidance.
 *
 * <p>A CASH tender may overpay (change is computed and returned); any other method may not pay
 * more than the outstanding balance. Undo only ever targets the most recent active tender (append
 * -only ledger — a reversed tender is flagged {@code isReversed}, never deleted, so the audit
 * trail is intact). Per-method surcharge computation (reference {@code _compute_surcharge_for} /
 * {@code _recompute_surcharge}) is deferred to the payment-methods slice, which is out of scope
 * here; this service still folds the header's existing surcharge fields into the balance check so
 * a surcharge already stamped by that slice is respected.
 */
@Service
public class TenderService {

    private final PosTenderRepository tenderRepository;
    private final TransactionCommandService transactions;
    private final AuditService auditService;

    public TenderService(PosTenderRepository tenderRepository, TransactionCommandService transactions,
                         AuditService auditService) {
        this.tenderRepository = tenderRepository;
        this.transactions = transactions;
        this.auditService = auditService;
    }

    /** US-033 FR-172..175 — add a payment leg; cash may overpay (change due). */
    @Transactional
    public PosTransactionResponse addTender(String publicId, TenderRequest req) {
        PosTransaction txn = transactions.load(publicId);
        assertOpen(txn);
        boolean isCash = req.isCash();

        long remaining = txn.getTotalAmount() - transactions.activePaidAmount(txn);
        if (!isCash && req.amount() > remaining) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Overpayment is not allowed for a non-cash tender (remaining " + remaining + ")");
        }

        long tendered;
        long applied;
        long change;
        if (isCash) {
            tendered = req.tenderedAmount() != null ? req.tenderedAmount() : req.amount();
            applied = remaining > 0 ? Math.min(tendered, remaining) : 0L;
            change = Math.max(0L, tendered - remaining);
        } else {
            tendered = req.amount();
            applied = req.amount();
            change = 0L;
        }

        int nextSeq = txn.getTenders().stream().mapToInt(PosTender::getSequence).max().orElse(0) + 1;
        PosTender tender = new PosTender();
        tender.setSequence(nextSeq);
        tender.setPaymentMethodId(req.paymentMethodId());
        tender.setMethodType(req.methodType());
        tender.setAmountAmount(applied);
        tender.setAmountCurrency(txn.getCurrency());
        tender.setTenderedAmount(isCash ? tendered : null);
        tender.setChangeAmount(change);
        tender.setReference(req.reference());
        tender.setMaskedPan(req.maskedPan());
        txn.addTender(tender);

        transactions.recomputeTotals(txn);
        PosTransaction saved = transactions.load(publicId);
        auditService.record(TransactionCommandService.AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE,
            null, transactions.response(saved));
        return transactions.response(saved);
    }

    /** FR-176 — undo the most recent (non-reversed) tender before completing. */
    @Transactional
    public PosTransactionResponse undoTender(String publicId, String tenderPublicId) {
        PosTransaction txn = transactions.load(publicId);
        assertOpen(txn);
        var active = txn.getTenders().stream().filter(t -> !t.isReversed()).toList();
        if (active.isEmpty()) {
            throw DomainException.notFound("PosTender", tenderPublicId);
        }
        PosTender target = tenderRepository.findByTransaction_PublicIdAndPublicId(publicId, tenderPublicId)
            .orElseThrow(() -> DomainException.notFound("PosTender", tenderPublicId));
        if (target.isReversed()) {
            throw DomainException.notFound("PosTender", tenderPublicId);
        }
        PosTender last = active.get(active.size() - 1);
        if (!last.getPublicId().equals(target.getPublicId())) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Only the most recent tender can be undone");
        }
        target.setReversed(true);

        transactions.recomputeTotals(txn);
        auditService.record(TransactionCommandService.AUDIT_ENTITY, txn.getPublicId(), AuditAction.UPDATE,
            null, transactions.response(txn));
        return transactions.response(txn);
    }

    private void assertOpen(PosTransaction txn) {
        if (txn.getStatus() != PosTransactionStatus.OPEN) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Transaction is not open (status " + txn.getStatus().name() + ")");
        }
    }
}

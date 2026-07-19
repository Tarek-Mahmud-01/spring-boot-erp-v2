package com.guru.erp.modules.pos.transactions.service;

import com.guru.erp.modules.pos.transactions.domain.PosTransaction;
import com.guru.erp.modules.pos.transactions.domain.PosTransactionStatus;
import com.guru.erp.modules.pos.transactions.dto.TransactionDtos.AgeVerifyRequest;
import com.guru.erp.modules.pos.transactions.dto.TransactionDtos.PosTransactionResponse;
import com.guru.erp.modules.pos.transactions.dto.TransactionDtos.VoidTransactionRequest;
import com.guru.erp.modules.pos.transactions.repository.PosTransactionRepository;
import com.guru.erp.platform.audit.AuditAction;
import com.guru.erp.platform.audit.AuditService;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import com.guru.erp.platform.security.CurrentUser;
import com.guru.erp.platform.status.StateMachine;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Status-workflow use-cases for the core POS sale (reference {@code app.pos.views}
 * {@code age_verify} / {@code complete_transaction} / {@code void_transaction} /
 * {@code park_transaction} / {@code resume_parked_sale}). Split out of
 * {@link TransactionCommandService} (cart-building) purely to keep each service focused and under
 * the size cap; both share the same {@link TransactionCommandService#WORKFLOW} status
 * {@link StateMachine} (OPEN → PARKED/COMPLETED/VOIDED, PARKED → OPEN, COMPLETED → VOIDED) and the
 * package-private {@code load}/{@code response}/{@code recomputeTotals} helpers.
 *
 * <p>Park/resume here is the bare status side-trip (OPEN ⇄ PARKED) on the transaction itself; the
 * short-code park-ticket / retention-window bookkeeping (reference {@code PosParkedSale}) is owned
 * by a separate "park" sub-slice, out of scope for this one. Completing a sale emits
 * {@code pos.sale.completed} (see {@link TransactionPostingService}) so inventory + GL react via
 * the outbox instead of a hard cross-slice call; voiding a previously-COMPLETED sale emits
 * {@code pos.sale.voided} to unwind those same effects.
 */
@Service
public class TransactionWorkflowService {

    private static final String DOCUMENT_TYPE_RECEIPT = "RECEIPT";
    private static final String RECEIPT_PREFIX = "R-";

    private final TransactionCommandService transactions;
    private final PosTransactionRepository repository;
    private final AuditService auditService;
    private final TransactionPostingService posting;
    private final CurrentUser currentUser;
    private final Clock clock = Clock.systemUTC();

    public TransactionWorkflowService(TransactionCommandService transactions,
                                      PosTransactionRepository repository,
                                      AuditService auditService, TransactionPostingService posting,
                                      CurrentUser currentUser) {
        this.transactions = transactions;
        this.repository = repository;
        this.auditService = auditService;
        this.posting = posting;
        this.currentUser = currentUser;
    }

    /** FR-AU-011/014 — stamp that the cashier verified the customer's age for a restricted line. */
    @Transactional
    public PosTransactionResponse ageVerify(String publicId, AgeVerifyRequest req) {
        PosTransaction txn = transactions.load(publicId);
        transactions.assertOpen(txn);
        txn.setAgeVerified(true);
        txn.setAgeVerifiedAt(Instant.now(clock));
        txn.setAgeVerifiedBy(actorId());
        txn.setAgeIdType(req.idType());
        PosTransaction saved = repository.save(txn);
        auditService.record(TransactionCommandService.AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE,
            null, transactions.response(saved));
        return transactions.response(saved);
    }

    /**
     * US-033 FR-173 — finalise a sale once the balance is zero. Enforces a non-empty cart, age
     * verification when a restricted line is present, and a zero balance, then stamps the receipt
     * number + completion timestamp and publishes {@code pos.sale.completed}.
     *
     * <p>Reference {@code _allocate_receipt_number} draws the number from the company's
     * NumberingRule (E-001, a settings-module concern); that facade is out of scope for this
     * slice, so this port always uses the reference's own interim fallback idea — a sequential
     * {@code R-<n>} number — rather than hard-calling the settings module.
     */
    @Transactional
    public PosTransactionResponse complete(String publicId) {
        PosTransaction txn = transactions.load(publicId);
        transactions.assertOpen(txn);
        if (txn.getLines().isEmpty()) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "Cannot complete an empty cart");
        }
        if (transactions.ageRequired(txn) && !txn.isAgeVerified()) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Age verification is required before this sale can be completed");
        }
        transactions.recomputeTotals(txn);
        long paid = transactions.activePaidAmount(txn);
        if (paid < txn.getTotalAmount()) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Payment is incomplete: balance " + (txn.getTotalAmount() - paid) + " remains");
        }

        long change = txn.getTenders().stream().filter(t -> !t.isReversed())
            .mapToLong(t -> t.getChangeAmount()).sum();
        txn.setChangeAmount(change);
        txn.setPaidAmount(paid);
        txn.setDocumentType(DOCUMENT_TYPE_RECEIPT);
        txn.setReceiptNumber(nextReceiptNumber());
        txn.setStatus(TransactionCommandService.WORKFLOW.transition(txn.getStatus(), PosTransactionStatus.COMPLETED));
        txn.setCompletedAt(Instant.now(clock));

        PosTransaction saved = repository.save(txn);
        auditService.record(TransactionCommandService.AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE,
            null, transactions.response(saved));
        posting.emitCompleted(saved);
        return transactions.response(saved);
    }

    /**
     * US-034 FR-181 — void a transaction. An OPEN cart is simply discarded; a COMPLETED sale is
     * fully reversed and {@code pos.sale.voided} lets downstream modules unwind inventory/GL/loyalty.
     */
    @Transactional
    public PosTransactionResponse voidTransaction(String publicId, VoidTransactionRequest req) {
        PosTransaction txn = transactions.load(publicId);
        if (txn.getStatus() != PosTransactionStatus.OPEN && txn.getStatus() != PosTransactionStatus.COMPLETED) {
            throw DomainException.illegalTransition(txn.getStatus().name(), PosTransactionStatus.VOIDED.name());
        }
        boolean wasCompleted = txn.getStatus() == PosTransactionStatus.COMPLETED;
        PosTransactionResponse before = transactions.response(txn);

        txn.getLines().forEach(line -> line.setVoidReason(req.reason()));
        txn.setStatus(TransactionCommandService.WORKFLOW.transition(txn.getStatus(), PosTransactionStatus.VOIDED));

        PosTransaction saved = repository.save(txn);
        auditService.record(TransactionCommandService.AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE,
            before, transactions.response(saved));
        if (wasCompleted) {
            posting.emitVoided(saved, true);
        }
        return transactions.response(saved);
    }

    /** US-035 FR-182/183 — park an OPEN cart (a non-empty, unpaid one) so the lane is freed. */
    @Transactional
    public PosTransactionResponse park(String publicId) {
        PosTransaction txn = transactions.load(publicId);
        transactions.assertOpen(txn);
        if (txn.getLines().isEmpty()) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "Cannot park an empty cart");
        }
        if (transactions.activePaidAmount(txn) > 0) {
            throw new DomainException(ErrorCode.CONFLICT, "Cannot park a cart that already has a payment");
        }
        PosTransactionResponse before = transactions.response(txn);
        txn.setStatus(TransactionCommandService.WORKFLOW.transition(txn.getStatus(), PosTransactionStatus.PARKED));
        PosTransaction saved = repository.save(txn);
        auditService.record(TransactionCommandService.AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE,
            before, transactions.response(saved));
        return transactions.response(saved);
    }

    /** US-035 FR-184/185 — resume a parked cart back to OPEN. */
    @Transactional
    public PosTransactionResponse resume(String publicId) {
        PosTransaction txn = transactions.load(publicId);
        if (txn.getStatus() != PosTransactionStatus.PARKED) {
            throw DomainException.illegalTransition(txn.getStatus().name(), PosTransactionStatus.OPEN.name());
        }
        PosTransactionResponse before = transactions.response(txn);
        txn.setStatus(TransactionCommandService.WORKFLOW.transition(txn.getStatus(), PosTransactionStatus.OPEN));
        PosTransaction saved = repository.save(txn);
        auditService.record(TransactionCommandService.AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE,
            before, transactions.response(saved));
        return transactions.response(saved);
    }

    private String actorId() {
        return currentUser.optional().map(p -> p.userPublicId()).orElse(null);
    }

    private String nextReceiptNumber() {
        long n = repository.countByReceiptNumberStartingWith(RECEIPT_PREFIX) + 1;
        return RECEIPT_PREFIX + String.format("%08d", n);
    }
}

package com.springboot.erp.modules.pos.transactions.service;

import com.springboot.erp.modules.pos.transactions.domain.PosTransaction;
import com.springboot.erp.modules.pos.transactions.domain.PosTransactionLine;
import com.springboot.erp.modules.pos.transactions.domain.PosTransactionStatus;
import com.springboot.erp.modules.pos.transactions.domain.PosTransactionType;
import com.springboot.erp.modules.pos.transactions.dto.TransactionDtos.AddLineRequest;
import com.springboot.erp.modules.pos.transactions.dto.TransactionDtos.AttachCustomerRequest;
import com.springboot.erp.modules.pos.transactions.dto.TransactionDtos.PosTransactionResponse;
import com.springboot.erp.modules.pos.transactions.dto.TransactionDtos.TransactionOpenRequest;
import com.springboot.erp.modules.pos.transactions.dto.TransactionDtos.UpdateLineRequest;
import com.springboot.erp.modules.pos.transactions.mapper.TransactionMapper;
import com.springboot.erp.modules.pos.transactions.repository.PosTransactionLineRepository;
import com.springboot.erp.modules.pos.transactions.repository.PosTransactionRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.security.CurrentUser;
import com.springboot.erp.platform.status.StateMachine;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side use-cases for building the cart of the core POS sale (US-031/032 — reference
 * {@code app.pos.views} {@code open_transaction} / {@code add_line_by_product} /
 * {@code set_line_qty} / {@code void_line}). Header+lines aggregate; one audit row per mutation.
 * The status-workflow moves (age-verify / complete / void / park / resume) live in
 * {@link TransactionWorkflowService}, which shares this service's {@link #load}/{@link #response}
 * /{@link #recomputeTotals} internals (package-private) to keep both under the service size cap.
 *
 * <p>Catalogue price/tax/promotion resolution (reference {@code product_views.resolve_sale_context})
 * and cart-level cross-sell/BXGY promotions are a Product-module concern; this slice's
 * {@link #addLine} takes the unit price / tax rate / restriction flags as already-resolved inputs
 * (a thin controller or a future product-facing façade supplies them) rather than hard-calling the
 * product slice, matching the "never hard-call another module" rule.
 */
@Service
public class TransactionCommandService {

    static final String AUDIT_ENTITY = "pos_transaction";

    /** Reference sale lifecycle (see {@link PosTransactionStatus} javadoc). Shared with the workflow service. */
    static final StateMachine<PosTransactionStatus> WORKFLOW = StateMachine.builder(PosTransactionStatus.class)
        .allow(PosTransactionStatus.OPEN, PosTransactionStatus.PARKED, PosTransactionStatus.COMPLETED,
            PosTransactionStatus.VOIDED)
        .allow(PosTransactionStatus.PARKED, PosTransactionStatus.OPEN)
        .allow(PosTransactionStatus.COMPLETED, PosTransactionStatus.VOIDED)
        .build();

    private final PosTransactionRepository repository;
    private final PosTransactionLineRepository lineRepository;
    private final TransactionMapper mapper;
    private final AuditService auditService;
    private final CurrentUser currentUser;
    private final Clock clock = Clock.systemUTC();

    public TransactionCommandService(PosTransactionRepository repository,
                                     PosTransactionLineRepository lineRepository,
                                     TransactionMapper mapper, AuditService auditService,
                                     CurrentUser currentUser) {
        this.repository = repository;
        this.lineRepository = lineRepository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    /** US-031 — open a new cart on a register. */
    @Transactional
    public PosTransactionResponse open(TransactionOpenRequest req) {
        PosTransaction txn = new PosTransaction();
        txn.setRegisterId(req.registerId());
        txn.setLocationId(req.locationId());
        txn.setCashierId(actorId());
        txn.setCustomerId(req.customerId());
        txn.setType(PosTransactionType.SALE);
        txn.setStatus(PosTransactionStatus.OPEN);
        txn.setCurrency(req.currency().toUpperCase());
        txn.setOccurredAt(Instant.now(clock));

        PosTransaction saved = repository.save(txn);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null, response(saved));
        return response(saved);
    }

    /** FR-066/FR-215 — attach (or detach) a customer on an open sale. */
    @Transactional
    public PosTransactionResponse setCustomer(String publicId, AttachCustomerRequest req) {
        PosTransaction txn = load(publicId);
        assertOpen(txn);
        PosTransactionResponse before = response(txn);
        txn.setCustomerId(req.customerId());
        PosTransaction saved = repository.save(txn);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before, response(saved));
        return response(saved);
    }

    /**
     * US-031 FR-164 / US-032 FR-171 — add a catalogue line to the cart. Price/tax/restriction are
     * SNAPSHOT onto the line at add time (reference: a later catalogue change never rewrites a
     * posted sale). Only a weighed product may carry a fractional {@code qty}. The request already
     * carries the resolved catalogue facts (see {@link AddLineRequest} javadoc) — this slice never
     * hard-calls the product module to resolve them itself.
     */
    @Transactional
    public PosTransactionResponse addLine(String publicId, AddLineRequest req) {
        PosTransaction txn = load(publicId);
        assertOpen(txn);
        if (req.qty().signum() <= 0) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "qty must be greater than zero");
        }
        if (!req.soldByWeight() && req.qty().stripTrailingZeros().scale() > 0) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Fractional quantities are only allowed for weighed goods");
        }
        long basePrice = req.basePriceAmount() != null ? req.basePriceAmount() : req.unitPriceAmount();
        LineMath.LineAmounts amounts = LineMath.compute(req.unitPriceAmount(), req.qty(), 0L,
            req.taxRatePercent(), req.taxInclusive());

        int nextLineNo = txn.getLines().stream().mapToInt(PosTransactionLine::getLineNo).max().orElse(0) + 1;
        PosTransactionLine line = new PosTransactionLine();
        line.setLineNo(nextLineNo);
        line.setProductId(req.productId());
        line.setVariantId(req.variantId());
        line.setSku(req.sku());
        line.setName(req.name());
        line.setBarcode(req.barcode());
        line.setQty(req.qty());
        line.setUnitPriceAmount(req.unitPriceAmount());
        line.setBasePriceAmount(basePrice);
        line.setCurrency(txn.getCurrency());
        line.setTaxCodeId(req.taxCodeId());
        line.setTaxRate(req.taxRatePercent());
        line.setTaxInclusive(req.taxInclusive());
        line.setTaxAmount(amounts.tax());
        line.setLineNetAmount(amounts.net());
        line.setLineTotalAmount(amounts.total());
        line.setRestricted18(req.isRestricted18());
        line.setRestricted21(req.isRestricted21());
        line.setRestrictedControlledDisplay(req.isRestrictedControlledDisplay());
        txn.addLine(line);

        recomputeTotals(txn);
        PosTransaction saved = repository.save(txn);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, null, response(saved));
        return response(saved);
    }

    /** FR-164 — change a line's quantity; FR-167 rejects qty &lt;= 0. */
    @Transactional
    public PosTransactionResponse setLineQty(String publicId, String lineId, UpdateLineRequest req) {
        PosTransaction txn = load(publicId);
        assertOpen(txn);
        if (req.qty().signum() <= 0) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "qty must be greater than zero");
        }
        PosTransactionLine line = lineRepository.findByTransaction_PublicIdAndPublicId(publicId, lineId)
            .orElseThrow(() -> DomainException.notFound("PosTransactionLine", lineId));
        if (line.getWeighedQtyKg() == null && req.qty().stripTrailingZeros().scale() > 0) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Fractional quantities are only allowed for weighed goods");
        }
        LineMath.LineAmounts amounts = LineMath.compute(line.getUnitPriceAmount(), req.qty(),
            line.getDiscountAmount(), line.getTaxRate(), line.isTaxInclusive());
        line.setQty(req.qty());
        line.setTaxAmount(amounts.tax());
        line.setLineNetAmount(amounts.net());
        line.setLineTotalAmount(amounts.total());

        recomputeTotals(txn);
        PosTransaction saved = repository.save(txn);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, null, response(saved));
        return response(saved);
    }

    /** FR-165 — void a line before payment; after payment requires a refund (a different sub-slice). */
    @Transactional
    public PosTransactionResponse voidLine(String publicId, String lineId) {
        PosTransaction txn = load(publicId);
        assertOpen(txn);
        if (activePaidAmount(txn) > 0) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Cannot remove a line once a payment has been tendered");
        }
        PosTransactionLine line = lineRepository.findByTransaction_PublicIdAndPublicId(publicId, lineId)
            .orElseThrow(() -> DomainException.notFound("PosTransactionLine", lineId));
        txn.getLines().remove(line);

        recomputeTotals(txn);
        PosTransaction saved = repository.save(txn);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, null, response(saved));
        return response(saved);
    }

    // --- internals (package-private: shared with TenderService / TransactionWorkflowService) -----

    /** Reference {@code _recompute_totals} (promotion/cross-sell passes are a Product-module concern). */
    void recomputeTotals(PosTransaction txn) {
        long goodsTotal = txn.getLines().stream().mapToLong(PosTransactionLine::getLineTotalAmount).sum();
        txn.setSubtotalAmount(txn.getLines().stream().mapToLong(PosTransactionLine::getLineNetAmount).sum());
        txn.setTaxAmount(txn.getLines().stream().mapToLong(PosTransactionLine::getTaxAmount).sum()
            + txn.getSurchargeTaxAmount());
        txn.setDiscountAmount(txn.getLines().stream().mapToLong(PosTransactionLine::getDiscountAmount).sum());
        txn.setTotalAmount(goodsTotal + txn.getSurchargeAmount() + txn.getSurchargeTaxAmount());
        txn.setPaidAmount(activePaidAmount(txn));
    }

    long activePaidAmount(PosTransaction txn) {
        return txn.getTenders().stream().filter(t -> !t.isReversed()).mapToLong(t -> t.getAmountAmount()).sum();
    }

    /** FR-AU-011 — a sale needs age verification if any line is age-restricted. */
    boolean ageRequired(PosTransaction txn) {
        return txn.getLines().stream().anyMatch(l -> l.isRestricted18() || l.isRestricted21());
    }

    PosTransaction load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("PosTransaction", publicId));
    }

    void assertOpen(PosTransaction txn) {
        if (txn.getStatus() != PosTransactionStatus.OPEN) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Transaction is not open (status " + txn.getStatus().name() + ")");
        }
    }

    private String actorId() {
        return currentUser.optional().map(p -> p.userPublicId()).orElse(null);
    }

    PosTransactionResponse response(PosTransaction txn) {
        return mapper.toResponse(txn, txn.getTotalAmount() - activePaidAmount(txn), ageRequired(txn));
    }
}

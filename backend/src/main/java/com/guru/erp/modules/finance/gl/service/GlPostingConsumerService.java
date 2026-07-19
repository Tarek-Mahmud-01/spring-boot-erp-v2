package com.guru.erp.modules.finance.gl.service;

import com.guru.erp.modules.finance.gl.domain.GlPostingLog;
import com.guru.erp.modules.finance.gl.domain.GlPostingStatus;
import com.guru.erp.modules.finance.gl.domain.HolderType;
import com.guru.erp.modules.finance.gl.domain.JournalEntry;
import com.guru.erp.modules.finance.gl.domain.JournalEntryStatus;
import com.guru.erp.modules.finance.gl.domain.JournalLine;
import com.guru.erp.modules.finance.gl.dto.PosSaleCompletedPayload;
import com.guru.erp.modules.finance.gl.mapper.GlMapper;
import com.guru.erp.modules.finance.gl.repository.GlPostingLogRepository;
import com.guru.erp.modules.finance.gl.repository.JournalEntryRepository;
import com.guru.erp.platform.audit.AuditAction;
import com.guru.erp.platform.audit.AuditService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * POS -&gt; GL integration, ported as an <b>outbox CONSUMER</b> (reference
 * {@code app.finance.integrations.pos_gl_poster.post_pos_voucher}) — NOT a hard call from the pos
 * module. The pos.transactions slice already publishes {@code pos.sale.completed} /
 * {@code pos.sale.voided} via {@code OutboxPublisher.publish(...)}; a future outbox-relay/dispatcher
 * layer (out of scope for this slice) will read those rows, deserialize them into
 * {@link PosSaleCompletedPayload}, and call {@link #postPosSale} / {@link #reversePosSale} here.
 * This service owns only the idempotent-posting half of that contract.
 *
 * <p><b>Idempotency</b>: every call first does a transactional check-then-insert against
 * {@link GlPostingLog} keyed on the natural key {@code (sourceKind="POS_SALE", sourceRef=transactionId)}
 * — check and insert happen inside the same {@code @Transactional} method as the journal-entry
 * write, so a replay of the same event (the outbox is at-least-once) can never double-post: a
 * pre-existing POSTED/SKIPPED row short-circuits before any {@link JournalEntry} is created, and the
 * unique {@code (sourceKind, sourceRef)} DB index (V71) is the final backstop if two replay threads
 * somehow race past the read at the same time. Log writes are delegated to {@link GlPostingLogWriter}
 * purely to keep this service under the size cap.
 *
 * <p>Posts a minimal two-leg voucher — Dr tender/cash account · Cr sales revenue (+ Cr GST payable
 * when {@code taxAmount > 0}) — because the full reference poster's COGS-at-moving-average-cost,
 * per-tender-method account resolution, and account-mapping lookups depend on slices
 * (chart-of-accounts, account-mapping, product-costing) not yet ported. The GL-core invariants this
 * slice owns are still fully enforced: the voucher is built already-balanced and POSTED atomically,
 * never left as an unbalanced DRAFT.
 */
@Service
public class GlPostingConsumerService {

    private static final Logger log = LoggerFactory.getLogger(GlPostingConsumerService.class);

    private static final String SOURCE_KIND_SALE = "POS_SALE";
    private static final String SOURCE_KIND_VOID = "POS_VOID";
    private static final String EVENT_SALE_COMPLETED = "pos.sale.completed";
    private static final String EVENT_SALE_VOIDED = "pos.sale.voided";
    private static final String VOUCHER_TYPE_SALE = "V-001";
    private static final String VOUCHER_TYPE_VOID = "V-005";

    /**
     * Placeholder GL accounts used until the account-mapping slice is ported. A real deployment
     * must replace these with the (company, module=POS, purpose) resolution the reference performs
     * via {@code account_mapping_service.get_mapped_account_public_id} — hardcoding here only keeps
     * this consumer runnable/testable in isolation, matching the instructions to port "a service
     * method that takes the already-parsed event payload and posts a journal entry" without
     * building the account-mapping infra itself.
     */
    private static final String PLACEHOLDER_CASH_ACCOUNT = "000000000000000000CASHACCT";
    private static final String PLACEHOLDER_REVENUE_ACCOUNT = "00000000000000SALESREVACCT";
    private static final String PLACEHOLDER_GST_ACCOUNT = "0000000000000000GSTPAYACCT";

    private final GlPostingLogRepository logRepository;
    private final JournalEntryRepository entryRepository;
    private final VoucherNumberingService numbering;
    private final BalancingService balancing;
    private final GlMapper mapper;
    private final AuditService auditService;
    private final GlPostingLogWriter logWriter;
    private final Clock clock = Clock.systemUTC();

    public GlPostingConsumerService(GlPostingLogRepository logRepository, JournalEntryRepository entryRepository,
                                    VoucherNumberingService numbering, BalancingService balancing,
                                    GlMapper mapper, AuditService auditService, GlPostingLogWriter logWriter) {
        this.logRepository = logRepository;
        this.entryRepository = entryRepository;
        this.numbering = numbering;
        this.balancing = balancing;
        this.mapper = mapper;
        this.auditService = auditService;
        this.logWriter = logWriter;
    }

    /**
     * Consume one {@code pos.sale.completed} event. Dr tender · Cr sales revenue · Cr GST payable.
     * No-op (SKIPPED) when the event carries no bookable amount, so a fully zero-value transaction
     * doesn't leave a broken DRAFT lying around.
     */
    @Transactional
    public void postPosSale(PosSaleCompletedPayload payload) {
        postOrSkip(SOURCE_KIND_SALE, payload.transactionId(), EVENT_SALE_COMPLETED, payload, false);
    }

    /** Consume a {@code pos.sale.voided} event for a previously-completed sale — mirror-image legs. */
    @Transactional
    public void reversePosSale(PosSaleCompletedPayload payload) {
        postOrSkip(SOURCE_KIND_VOID, payload.transactionId(), EVENT_SALE_VOIDED, payload, true);
    }

    private void postOrSkip(String sourceKind, String sourceRef, String eventType,
                           PosSaleCompletedPayload payload, boolean reverseDirection) {
        if (sourceRef == null || sourceRef.isBlank()) {
            log.warn("GL consumer: event {} has no source ref; skipping", eventType);
            return;
        }

        // Check-then-insert, same transaction as the journal write: the idempotency gate.
        GlPostingLog existing = logRepository.findBySourceKindAndSourceRef(sourceKind, sourceRef).orElse(null);
        if (existing != null
            && (existing.getStatus() == GlPostingStatus.POSTED || existing.getStatus() == GlPostingStatus.SKIPPED)) {
            log.info("GL consumer: {} {} already {}; skipping replay", sourceKind, sourceRef, existing.getStatus());
            return;
        }

        long net = payload.netAmount();
        long tax = payload.taxAmount();
        long tender = payload.tenderAmount();
        boolean bookable = (net + tax) > 0 && tender > 0;
        if (!bookable) {
            logWriter.record(existing, payload.companyId(), sourceKind, sourceRef, eventType,
                GlPostingStatus.SKIPPED, null, payload.requestId());
            return;
        }

        JournalEntry entry = buildEntry(payload, sourceRef, reverseDirection);
        addLegs(entry, payload, reverseDirection);

        long totalDebit = entry.getLines().stream().mapToLong(JournalLine::getBaseDebit).sum();
        long totalCredit = entry.getLines().stream().mapToLong(JournalLine::getBaseCredit).sum();
        balancing.assertBalanced(totalDebit, totalCredit);
        entry.setTotalDebit(totalDebit);
        entry.setTotalCredit(totalCredit);

        if (entry.getLines().size() < 2) {
            logWriter.record(existing, payload.companyId(), sourceKind, sourceRef, eventType,
                GlPostingStatus.SKIPPED, null, payload.requestId());
            return;
        }

        try {
            JournalEntry saved = entryRepository.save(entry);
            auditService.record(JournalEntryCommandService.AUDIT_ENTITY, saved.getPublicId(),
                AuditAction.CREATE, null, mapper.toResponse(saved));
            logWriter.record(existing, payload.companyId(), sourceKind, sourceRef, eventType,
                GlPostingStatus.POSTED, saved.getPublicId(), payload.requestId());
        } catch (RuntimeException ex) {
            logWriter.recordFailure(existing, payload.companyId(), sourceKind, sourceRef, eventType, ex,
                payload.requestId());
            throw ex;
        }
    }

    private JournalEntry buildEntry(PosSaleCompletedPayload payload, String sourceRef, boolean reverseDirection) {
        JournalEntry entry = new JournalEntry();
        entry.setCompanyId(payload.companyId());
        entry.setLocationId(payload.locationId());
        String voucherType = reverseDirection ? VOUCHER_TYPE_VOID : VOUCHER_TYPE_SALE;
        entry.setVoucherType(voucherType);
        var entryDate = (payload.entryDate() != null ? payload.entryDate() : Instant.now(clock))
            .atZone(ZoneOffset.UTC).toLocalDate();
        entry.setVoucherNumber(numbering.next(payload.companyId(), voucherType, entryDate));
        entry.setEntryDate(entryDate);
        entry.setPeriodCode(JournalEntryCommandService.periodCodeOf(entryDate));
        entry.setReference(sourceRef);
        entry.setNarration((reverseDirection ? "POS void " : "POS sale ") + sourceRef);
        entry.setStatus(JournalEntryStatus.POSTED);
        entry.setPostedAt(Instant.now(clock));
        entry.setPostedBy(null); // system-attributed posting; no interactive actor on the outbox path
        return entry;
    }

    private void addLegs(JournalEntry entry, PosSaleCompletedPayload payload, boolean reverseDirection) {
        long net = payload.netAmount();
        long tax = payload.taxAmount();
        long tender = payload.tenderAmount();
        String currency = payload.currency();

        long tenderDebit = reverseDirection ? 0 : tender;
        long tenderCredit = reverseDirection ? tender : 0;
        addLine(entry, PLACEHOLDER_CASH_ACCOUNT, tenderDebit, tenderCredit, currency, "POS tender");

        if (net > 0) {
            long revenueDebit = reverseDirection ? net : 0;
            long revenueCredit = reverseDirection ? 0 : net;
            addLine(entry, PLACEHOLDER_REVENUE_ACCOUNT, revenueDebit, revenueCredit, currency, "Sales revenue");
        }
        if (tax > 0) {
            long taxDebit = reverseDirection ? tax : 0;
            long taxCredit = reverseDirection ? 0 : tax;
            addLine(entry, PLACEHOLDER_GST_ACCOUNT, taxDebit, taxCredit, currency, "GST payable");
        }
    }

    private void addLine(JournalEntry entry, String accountId, long debit, long credit, String currency,
                        String narration) {
        if (debit == 0 && credit == 0) {
            return;
        }
        balancing.assertXor(debit, credit);
        JournalLine line = new JournalLine();
        line.setLineNo(entry.getLines().size() + 1);
        line.setAccountId(accountId);
        line.setHolderType(HolderType.NONE);
        line.setNarration(narration);
        line.setDebit(debit);
        line.setCredit(credit);
        line.setCurrency(currency != null ? currency : "USD");
        line.setExchangeRate(java.math.BigDecimal.ONE);
        line.setBaseDebit(debit);
        line.setBaseCredit(credit);
        line.setLocationId(entry.getLocationId());
        entry.addLine(line);
    }
}

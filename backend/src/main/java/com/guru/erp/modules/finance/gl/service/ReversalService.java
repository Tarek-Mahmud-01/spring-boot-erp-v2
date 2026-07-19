package com.guru.erp.modules.finance.gl.service;

import com.guru.erp.modules.finance.gl.domain.JournalEntry;
import com.guru.erp.modules.finance.gl.domain.JournalEntryStatus;
import com.guru.erp.modules.finance.gl.domain.JournalLine;
import com.guru.erp.modules.finance.gl.dto.JournalEntryDtos.JournalEntryReverseRequest;
import com.guru.erp.modules.finance.gl.dto.JournalEntryDtos.JournalEntryResponse;
import com.guru.erp.modules.finance.gl.mapper.GlMapper;
import com.guru.erp.modules.finance.gl.repository.JournalEntryRepository;
import com.guru.erp.platform.audit.AuditAction;
import com.guru.erp.platform.audit.AuditService;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import com.guru.erp.platform.security.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * POSTED -&gt; REVERSED (reference {@code reverse_journal_entry}). Reversing creates a brand-new,
 * already-POSTED mirror-image entry (every line's debit/credit swapped, including the
 * base-currency amounts and the original's exchange rate carried across unchanged — reference QA
 * note C13: swapping only debit/credit and leaving base_debit/base_credit at their zero default
 * would leave a multi-currency reversal unbalanced in the base ledger) and links the two via
 * {@code reversedById}. Both the original and the reversal are then immutable: the original moves
 * to REVERSED (a terminal state — {@link PostingService#WORKFLOW} has no transition out of it) and
 * the reversal is born already POSTED.
 *
 * <p>A POSTED entry may be reversed at most once — a second reversal attempt on an
 * already-{@code reversedById != null} entry is rejected, mirroring the reference's
 * {@code JournalAlreadyReversedError}.
 */
@Service
public class ReversalService {

    private static final String REVERSAL_VOUCHER_TYPE = "V-005";

    private final JournalEntryRepository repository;
    private final VoucherNumberingService numbering;
    private final GlMapper mapper;
    private final AuditService auditService;
    private final CurrentUser currentUser;
    private final Clock clock = Clock.systemUTC();

    public ReversalService(JournalEntryRepository repository, VoucherNumberingService numbering,
                           GlMapper mapper, AuditService auditService, CurrentUser currentUser) {
        this.repository = repository;
        this.numbering = numbering;
        this.mapper = mapper;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    @Transactional
    public JournalEntryResponse reverse(String publicId, JournalEntryReverseRequest req) {
        JournalEntry original = repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("JournalEntry", publicId));
        if (original.getStatus() != JournalEntryStatus.POSTED) {
            throw new DomainException(ErrorCode.ILLEGAL_STATE_TRANSITION,
                "Journal entry " + publicId + " is " + original.getStatus() + "; only a POSTED entry may be reversed");
        }
        if (original.getReversedById() != null) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Journal entry " + publicId + " has already been reversed");
        }

        String narration = req.narration() != null
            ? req.narration()
            : "Reversal of " + original.getVoucherNumber() + ": " + original.getNarration();

        JournalEntry reversal = new JournalEntry();
        reversal.setCompanyId(original.getCompanyId());
        reversal.setLocationId(original.getLocationId());
        reversal.setVoucherType(REVERSAL_VOUCHER_TYPE);
        reversal.setVoucherNumber(numbering.next(original.getCompanyId(), REVERSAL_VOUCHER_TYPE, req.entryDate()));
        reversal.setEntryDate(req.entryDate());
        reversal.setPeriodCode(JournalEntryCommandService.periodCodeOf(req.entryDate()));
        reversal.setReference(original.getVoucherNumber());
        reversal.setNarration(narration);
        reversal.setStatus(JournalEntryStatus.POSTED);
        reversal.setTotalDebit(original.getTotalCredit());
        reversal.setTotalCredit(original.getTotalDebit());
        reversal.setPostedAt(Instant.now(clock));
        reversal.setPostedBy(currentUser.optional().map(p -> p.userPublicId()).orElse(null));

        for (JournalLine src : original.getLines()) {
            JournalLine mirror = new JournalLine();
            mirror.setLineNo(src.getLineNo());
            mirror.setAccountId(src.getAccountId());
            mirror.setHolderType(src.getHolderType());
            mirror.setHolderId(src.getHolderId());
            mirror.setNarration("Reversal: " + src.getNarration());
            // Swap debit <-> credit, transaction-currency AND base-currency amounts alike, so a
            // multi-currency reversal still balances in the company base currency.
            mirror.setDebit(src.getCredit());
            mirror.setCredit(src.getDebit());
            mirror.setCurrency(src.getCurrency());
            mirror.setExchangeRate(src.getExchangeRate());
            mirror.setBaseDebit(src.getBaseCredit());
            mirror.setBaseCredit(src.getBaseDebit());
            mirror.setLocationId(src.getLocationId());
            reversal.addLine(mirror);
        }

        original.setStatus(JournalEntryStatus.REVERSED);

        JournalEntry savedReversal = repository.save(reversal);
        // reversedById is a loose same-table reference by public id, set only once the reversal has one.
        original.setReversedById(savedReversal.getPublicId());
        JournalEntry savedOriginal = repository.save(original);

        JournalEntryResponse reversalResponse = mapper.toResponse(savedReversal);
        auditService.record(JournalEntryCommandService.AUDIT_ENTITY, savedReversal.getPublicId(),
            AuditAction.CREATE, null, reversalResponse);
        auditService.record(JournalEntryCommandService.AUDIT_ENTITY, savedOriginal.getPublicId(),
            AuditAction.UPDATE,
            java.util.Map.of("status", "POSTED"),
            java.util.Map.of("status", "REVERSED", "reversedById", savedReversal.getPublicId()));
        return reversalResponse;
    }
}

package com.guru.erp.modules.finance.gl.service;

import com.guru.erp.modules.finance.gl.domain.JournalEntry;
import com.guru.erp.modules.finance.gl.domain.JournalEntryStatus;
import com.guru.erp.modules.finance.gl.dto.JournalEntryDtos.JournalEntryCreateRequest;
import com.guru.erp.modules.finance.gl.dto.JournalEntryDtos.JournalEntryResponse;
import com.guru.erp.modules.finance.gl.dto.JournalEntryDtos.JournalEntryUpdateRequest;
import com.guru.erp.modules.finance.gl.mapper.GlMapper;
import com.guru.erp.modules.finance.gl.repository.JournalEntryRepository;
import com.guru.erp.platform.audit.AuditAction;
import com.guru.erp.platform.audit.AuditService;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Create / update / delete a DRAFT {@link JournalEntry} (reference {@code views/journal_entries.py}
 * {@code create_journal_entry} / {@code update_journal_entry} / {@code delete_journal_entry}). Only
 * a DRAFT entry may be mutated or deleted — once POSTED, the only legal moves are
 * {@code PostingService.post}'s already-applied transition and {@code ReversalService.reverse}.
 */
@Service
public class JournalEntryCommandService {

    static final String AUDIT_ENTITY = "journal_entry";

    private final JournalEntryRepository repository;
    private final BalancingService balancing;
    private final JournalLineFactory lineFactory;
    private final VoucherNumberingService numbering;
    private final GlMapper mapper;
    private final AuditService auditService;

    public JournalEntryCommandService(JournalEntryRepository repository, BalancingService balancing,
                                      JournalLineFactory lineFactory, VoucherNumberingService numbering,
                                      GlMapper mapper, AuditService auditService) {
        this.repository = repository;
        this.balancing = balancing;
        this.lineFactory = lineFactory;
        this.numbering = numbering;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    /** Create a new DRAFT entry. Lines must already balance — see {@link BalancingService}. */
    @Transactional
    public JournalEntryResponse create(JournalEntryCreateRequest req) {
        balancing.assertBalancedRequest(req.lines());

        JournalEntry entry = new JournalEntry();
        entry.setCompanyId(req.companyId());
        entry.setLocationId(req.locationId());
        entry.setVoucherType(req.voucherType());
        entry.setVoucherNumber(numbering.next(req.companyId(), req.voucherType(), req.entryDate()));
        entry.setEntryDate(req.entryDate());
        entry.setPeriodCode(periodCodeOf(req.entryDate()));
        entry.setReference(req.reference());
        entry.setNarration(req.narration() != null ? req.narration() : "");
        entry.setStatus(JournalEntryStatus.DRAFT);

        lineFactory.appendLines(entry, req.lines());
        long totalDebit = entry.getLines().stream().mapToLong(l -> l.getBaseDebit()).sum();
        long totalCredit = entry.getLines().stream().mapToLong(l -> l.getBaseCredit()).sum();
        balancing.assertBalanced(totalDebit, totalCredit);
        entry.setTotalDebit(totalDebit);
        entry.setTotalCredit(totalCredit);

        JournalEntry saved = repository.save(entry);
        JournalEntryResponse response = mapper.toResponse(saved);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null, response);
        return response;
    }

    /** Full line replacement + header edit on a DRAFT entry — a draft has no ledger impact yet. */
    @Transactional
    public JournalEntryResponse update(String publicId, JournalEntryUpdateRequest req) {
        JournalEntry entry = load(publicId);
        assertVersion(entry, req.version());
        assertDraft(entry);

        JournalEntryResponse before = mapper.toResponse(entry);

        if (req.entryDate() != null) {
            entry.setEntryDate(req.entryDate());
            entry.setPeriodCode(periodCodeOf(req.entryDate()));
        }
        if (req.reference() != null) {
            entry.setReference(req.reference());
        }
        if (req.narration() != null) {
            entry.setNarration(req.narration());
        }
        if (req.voucherType() != null) {
            entry.setVoucherType(req.voucherType());
        }
        if (req.locationId() != null) {
            entry.setLocationId(req.locationId());
        }
        if (req.lines() != null) {
            balancing.assertBalancedRequest(req.lines());
            entry.getLines().clear();
            lineFactory.appendLines(entry, req.lines());
            long totalDebit = entry.getLines().stream().mapToLong(l -> l.getBaseDebit()).sum();
            long totalCredit = entry.getLines().stream().mapToLong(l -> l.getBaseCredit()).sum();
            balancing.assertBalanced(totalDebit, totalCredit);
            entry.setTotalDebit(totalDebit);
            entry.setTotalCredit(totalCredit);
        }

        JournalEntry saved = repository.save(entry);
        JournalEntryResponse after = mapper.toResponse(saved);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before, after);
        return after;
    }

    /** Soft-delete a DRAFT entry (reference "Audit C5" — hidden from list/get, never physically removed). */
    @Transactional
    public void delete(String publicId) {
        JournalEntry entry = load(publicId);
        assertDraft(entry);
        JournalEntryResponse before = mapper.toResponse(entry);
        entry.softDelete();
        repository.save(entry);
        auditService.record(AUDIT_ENTITY, entry.getPublicId(), AuditAction.DELETE, before, null);
    }

    JournalEntry load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("JournalEntry", publicId));
    }

    static void assertDraft(JournalEntry entry) {
        if (entry.getStatus() != JournalEntryStatus.DRAFT) {
            throw new DomainException(ErrorCode.ILLEGAL_STATE_TRANSITION,
                "Journal entry " + entry.getPublicId() + " is " + entry.getStatus()
                    + "; only a DRAFT entry may be edited or deleted");
        }
    }

    static void assertVersion(JournalEntry entry, long expected) {
        if (entry.getVersion() != expected) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK,
                "Journal entry " + entry.getPublicId() + " was modified concurrently "
                    + "(expected version " + expected + ", was " + entry.getVersion() + ")");
        }
    }

    static String periodCodeOf(java.time.LocalDate date) {
        return "%04d-%02d".formatted(date.getYear(), date.getMonthValue());
    }
}

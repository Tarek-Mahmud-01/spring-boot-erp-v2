package com.springboot.erp.modules.finance.gl.service;

import com.springboot.erp.modules.finance.gl.domain.JournalEntry;
import com.springboot.erp.modules.finance.gl.domain.JournalEntryStatus;
import com.springboot.erp.modules.finance.gl.dto.JournalEntryDtos.JournalEntryResponse;
import com.springboot.erp.modules.finance.gl.mapper.GlMapper;
import com.springboot.erp.modules.finance.gl.repository.JournalEntryRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.security.CurrentUser;
import com.springboot.erp.platform.status.StateMachine;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DRAFT -&gt; POSTED transition (reference {@code post_journal_entry} / {@code post_system_voucher}).
 *
 * <p>A JournalEntry may only be posted if {@code sum(lines.baseDebit) == sum(lines.baseCredit)}
 * EXACTLY (the balanced-voucher invariant, re-checked here — not just trusted from create — because
 * the entry may have been edited since). Never silently rounds; throws a
 * {@code DomainException(VALIDATION_FAILED, ...)} on any imbalance instead. Once posted, the entry
 * (and every one of its lines) is immutable: the state machine only allows POSTED -&gt; REVERSED from
 * here on ({@link ReversalService}), never back to DRAFT and never a further edit.
 */
@Service
public class PostingService {

    static final StateMachine<JournalEntryStatus> WORKFLOW = StateMachine.builder(JournalEntryStatus.class)
        .allow(JournalEntryStatus.DRAFT, JournalEntryStatus.POSTED)
        .allow(JournalEntryStatus.POSTED, JournalEntryStatus.REVERSED)
        .build();

    private final JournalEntryRepository repository;
    private final BalancingService balancing;
    private final GlMapper mapper;
    private final AuditService auditService;
    private final CurrentUser currentUser;
    private final Clock clock = Clock.systemUTC();

    public PostingService(JournalEntryRepository repository, BalancingService balancing, GlMapper mapper,
                          AuditService auditService, CurrentUser currentUser) {
        this.repository = repository;
        this.balancing = balancing;
        this.mapper = mapper;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    @Transactional
    public JournalEntryResponse post(String publicId) {
        JournalEntry entry = repository.findByPublicId(publicId)
            .orElseThrow(() -> com.springboot.erp.platform.error.DomainException.notFound("JournalEntry", publicId));
        JournalEntryResponse before = mapper.toResponse(entry);

        long totalDebit = entry.getLines().stream().mapToLong(l -> l.getBaseDebit()).sum();
        long totalCredit = entry.getLines().stream().mapToLong(l -> l.getBaseCredit()).sum();
        balancing.assertBalanced(totalDebit, totalCredit);

        entry.setStatus(WORKFLOW.transition(entry.getStatus(), JournalEntryStatus.POSTED));
        entry.setTotalDebit(totalDebit);
        entry.setTotalCredit(totalCredit);
        entry.setPostedAt(Instant.now(clock));
        entry.setPostedBy(currentUser.optional().map(p -> p.userPublicId()).orElse(null));

        JournalEntry saved = repository.save(entry);
        JournalEntryResponse after = mapper.toResponse(saved);
        auditService.record(JournalEntryCommandService.AUDIT_ENTITY, saved.getPublicId(),
            AuditAction.UPDATE, before, after);
        return after;
    }
}

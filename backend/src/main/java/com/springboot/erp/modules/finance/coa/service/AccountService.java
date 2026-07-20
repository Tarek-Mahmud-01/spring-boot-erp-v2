package com.springboot.erp.modules.finance.coa.service;

import com.springboot.erp.modules.finance.coa.domain.Account;
import com.springboot.erp.modules.finance.coa.domain.AccountPostingType;
import com.springboot.erp.modules.finance.coa.domain.AccountStatus;
import com.springboot.erp.modules.finance.coa.dto.AccountDtos.AccountCreateRequest;
import com.springboot.erp.modules.finance.coa.dto.AccountDtos.AccountResponse;
import com.springboot.erp.modules.finance.coa.dto.AccountDtos.AccountUpdateRequest;
import com.springboot.erp.modules.finance.coa.dto.AccountDtos.MoveAccountRequest;
import com.springboot.erp.modules.finance.coa.mapper.CoaMapper;
import com.springboot.erp.modules.finance.coa.repository.AccountRepository;
import com.springboot.erp.modules.finance.coa.service.NestedSetService.NestedSetSlot;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.web.PageResponse;
import java.util.Locale;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD + tree-mutation use-cases for {@link Account} (reference
 * {@code views/chart_of_accounts.py} ChartOfAccountsView). Every insert/delete/
 * move delegates the lft/rgt/depth bookkeeping to {@link NestedSetService} so
 * this service only ever touches ONE row's business fields directly plus the
 * incremental shift — never a full-tree rebuild.
 *
 * <p>NOTE — scope: the reference's create path also auto-posts a balanced
 * V-017 "opening balance" JournalEntry when {@code openingDebitAmount}/
 * {@code openingCreditAmount} is supplied (pairing the new account against the
 * OPENING_BALANCE_EQUITY mapping). JournalEntry/JournalLine belong to a
 * separate finance sub-slice (not ported here); this service persists the
 * opening-balance fields on the Account row (so the UI can still show what was
 * seeded) but does NOT auto-post a journal entry — that responsibility is left
 * for the journal sub-slice to wire in via its own service, exactly like the
 * outbox-consumption seam described for POS -> GL postings.
 */
@Service
public class AccountService {

    private static final String AUDIT_ENTITY = "account";

    private final AccountRepository repository;
    private final NestedSetService nestedSetService;
    private final CoaMapper mapper;
    private final AuditService auditService;

    public AccountService(AccountRepository repository, NestedSetService nestedSetService,
                          CoaMapper mapper, AuditService auditService) {
        this.repository = repository;
        this.nestedSetService = nestedSetService;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AccountResponse> list(String companyId, String q, Pageable pageable) {
        var page = q == null || q.isBlank()
            ? repository.findByCompanyId(companyId, pageable)
            : repository.search(companyId, "%" + q.trim().toLowerCase(Locale.ROOT) + "%", pageable);
        return PageResponse.of(page, this::toResponse);
    }

    @Transactional(readOnly = true)
    public AccountResponse get(String publicId) {
        return toResponse(load(publicId));
    }

    @Transactional
    public AccountResponse create(AccountCreateRequest req) {
        String code = req.code().trim();
        repository.findByCompanyIdAndCodeAndDeletedAtIsNull(req.companyId(), code)
            .ifPresent(a -> {
                throw new DomainException(ErrorCode.DUPLICATE,
                    "Account code already exists for this company: " + code);
            });

        Account parent = resolveParent(req.companyId(), req.parentId());
        assertParentIsHeader(parent);

        long obDr = Math.max(req.openingDebitAmount(), 0);
        long obCr = Math.max(req.openingCreditAmount(), 0);
        if (obDr > 0 && obCr > 0) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Provide either openingDebitAmount OR openingCreditAmount, not both.");
        }

        // Reserve the lft/rgt/depth slot via the incremental shift BEFORE
        // inserting the row, so the new node lands in a consistent slot the
        // same transaction sees — never a full-tree rebuild.
        NestedSetSlot slot = nestedSetService.reserveInsertSlot(req.companyId(), parent);

        Account a = new Account();
        a.setCompanyId(req.companyId());
        a.setCode(code);
        a.setName(req.name().trim());
        a.setType(req.type());
        a.setParent(parent);
        a.setPostingType(req.postingType() != null ? req.postingType() : AccountPostingType.POSTING);
        a.setCurrency(req.currency() != null ? req.currency().toUpperCase(Locale.ROOT) : null);
        a.setStatus(AccountStatus.ACTIVE);
        a.setOpeningDebitAmount(obDr);
        a.setOpeningCreditAmount(obCr);
        a.setOpeningDate(req.openingDate());
        a.setOpeningLocationId(req.openingLocationId());
        a.setLft(slot.lft());
        a.setRgt(slot.rgt());
        a.setDepth(slot.depth());

        Account saved = repository.save(a);
        AccountResponse response = toResponse(saved);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null, response);
        return response;
    }

    @Transactional
    public AccountResponse update(String publicId, AccountUpdateRequest req) {
        Account a = load(publicId);
        checkVersion(a, req.version());
        AccountResponse before = toResponse(a);

        if (req.name() != null) {
            a.setName(req.name().trim());
        }
        if (req.postingType() != null) {
            // FR-224: a HEADER that still has children cannot be demoted to
            // POSTING — that yields a postable account which is also a parent.
            if (req.postingType() == AccountPostingType.POSTING
                && a.getPostingType() == AccountPostingType.HEADER
                && repository.existsByParentIdAndDeletedAtIsNull(a.getId())) {
                throw new DomainException(ErrorCode.CONFLICT,
                    "Cannot demote a HEADER account to POSTING while it still has child accounts.");
            }
            a.setPostingType(req.postingType());
        }
        if (req.currency() != null) {
            a.setCurrency(req.currency().isBlank() ? null : req.currency().toUpperCase(Locale.ROOT));
        }
        if (req.status() != null) {
            a.setStatus(req.status());
        }
        // Renames / status / currency / posting_type changes never move nodes —
        // the parentId re-parent path lives in moveAccount() below, exactly
        // like the reference splits "update fields" from "move_subtree".

        Account saved = repository.save(a);
        AccountResponse response = toResponse(saved);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before, response);
        return response;
    }

    /**
     * FR-223 — re-parent an account (and its whole subtree). Dedicated
     * endpoint so the move always goes through {@link NestedSetService#moveSubtree}
     * explicitly, mirroring the reference's separation of "field update" from
     * "subtree move" inside {@code update_account}.
     */
    @Transactional
    public AccountResponse moveAccount(String publicId, MoveAccountRequest req) {
        Account a = load(publicId);
        checkVersion(a, req.version());
        AccountResponse before = toResponse(a);

        Account newParent = null;
        if (req.newParentId() != null && !req.newParentId().isBlank()) {
            newParent = repository.findByPublicId(req.newParentId())
                .orElseThrow(() -> DomainException.notFound("Account", req.newParentId()));
            if (newParent.getId().equals(a.getId())) {
                throw new DomainException(ErrorCode.VALIDATION_FAILED, "An account cannot be its own parent.");
            }
            assertParentIsHeader(newParent);
        }

        Long newParentId = newParent != null ? newParent.getId() : null;
        nestedSetService.moveSubtree(a.getCompanyId(), a, newParent);

        // The bulk shifts above clear the persistence context (see
        // AccountRepository javadoc) — re-load both the moved node and its new
        // parent (if any) by id rather than reusing the now-detached instances,
        // then set parent_id explicitly (parent_id itself is never touched by
        // the nested-set shift SQL, only lft/rgt/depth are).
        Account reloaded = repository.findById(a.getId())
            .orElseThrow(() -> DomainException.notFound("Account", publicId));
        Account reloadedParent = newParentId != null
            ? repository.findById(newParentId).orElseThrow(() -> DomainException.notFound("Account", newParentId.toString()))
            : null;
        reloaded.setParent(reloadedParent);
        Account saved = repository.save(reloaded);
        AccountResponse response = toResponse(saved);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before, response);
        return response;
    }

    /** FR-226 / AC-043-4: delete blocked when any postings or children exist. */
    @Transactional
    public void delete(String publicId) {
        Account a = load(publicId);

        if (repository.existsByParentIdAndDeletedAtIsNull(a.getId())) {
            throw new DomainException(ErrorCode.CONFLICT, "Cannot delete an account that still has child accounts.");
        }
        // NOTE: "has postings" (journal_lines.account_id references) is a
        // cross-slice check the journal sub-slice owns; this coa-only port
        // has no JournalLine table to query. Callers that DO have journal
        // posting data available should check it before invoking delete.

        AccountResponse before = toResponse(a);
        String companyId = a.getCompanyId();
        int oldLft = a.getLft();
        int oldRgt = a.getRgt();

        repository.delete(a);
        auditService.record(AUDIT_ENTITY, publicId, AuditAction.DELETE, before, null);

        // Close the gap AFTER the row is gone — mirrors the reference's
        // ordering (db.delete -> flush -> write_audit -> close_delete_gap).
        nestedSetService.closeDeleteGap(companyId, oldLft, oldRgt);
    }

    private Account resolveParent(String companyId, String parentPublicId) {
        if (parentPublicId == null || parentPublicId.isBlank()) {
            return null;
        }
        Account parent = repository.findByPublicId(parentPublicId)
            .orElseThrow(() -> DomainException.notFound("Account", parentPublicId));
        if (!parent.getCompanyId().equals(companyId)) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Parent account must belong to the same company.");
        }
        return parent;
    }

    private void assertParentIsHeader(Account parent) {
        if (parent != null && parent.getPostingType() != AccountPostingType.HEADER) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Parent account must be a HEADER account, not a POSTING leaf.");
        }
    }

    /** AC-043-2 — caller (journal / sales / procurement posting services) must invoke before posting. */
    @Transactional(readOnly = true)
    public Account assertAccountPostable(String publicId) {
        Account a = load(publicId);
        if (a.getPostingType() != AccountPostingType.POSTING) {
            throw new DomainException(ErrorCode.CONFLICT, "Account is a HEADER account and cannot be posted to.");
        }
        if (a.getStatus() != AccountStatus.ACTIVE) {
            throw new DomainException(ErrorCode.CONFLICT, "Account is inactive and cannot be posted to.");
        }
        return a;
    }

    private Account load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("Account", publicId));
    }

    private void checkVersion(Account a, Long requestVersion) {
        if (requestVersion != null && requestVersion != a.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK, ErrorCode.OPTIMISTIC_LOCK.defaultDetail());
        }
    }

    private AccountResponse toResponse(Account a) {
        String parentPublicId = a.getParent() != null ? a.getParent().getPublicId() : null;
        return mapper.toResponse(a, parentPublicId);
    }
}

package com.springboot.erp.modules.reporting.finance.service;

import com.springboot.erp.modules.finance.coa.domain.Account;
import com.springboot.erp.modules.finance.coa.domain.AccountPostingType;
import com.springboot.erp.modules.finance.coa.domain.QAccount;
import com.springboot.erp.modules.finance.coa.repository.AccountRepository;
import com.springboot.erp.modules.finance.gl.domain.QJournalLine;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.stereotype.Component;

/**
 * Shared scope-resolution helpers for every Finance report query service.
 *
 * <p>Reference parity notes (see {@code app.reports.repositories.finance}):
 * <ul>
 *   <li>{@code company_id} is a loose cross-module ULID everywhere in this codebase (unlike the
 *       reference's bigint company FK) — reports simply filter on the string id directly, so there
 *       is no separate "resolve company ULID -&gt; bigint" round trip to port.</li>
 *   <li>The reference's nested-set account-window predicate (a HEADER/group account sums every
 *       POSTING descendant via {@code lft}/{@code rgt} containment) IS ported — see
 *       {@link #accountScopePredicate(Account)}.</li>
 *   <li>{@code location_id} filters are simply compared as ULID strings (loose reference) — no
 *       existence probe, since the target project's Location module lives outside this slice's
 *       reach and a typo'd id should just yield an empty report, not a 404, consistent with how
 *       every other cross-module ULID filter in this codebase behaves.</li>
 * </ul>
 */
@Component
public class FinanceReportSupport {

    private final AccountRepository accountRepository;

    public FinanceReportSupport(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /** Resolve an account by its public ULID, scoped to the given company. 404 if missing/foreign. */
    public Account requireAccount(String companyId, String accountPublicId) {
        Account account = accountRepository.findByPublicId(accountPublicId)
            .orElseThrow(() -> DomainException.notFound("Account", accountPublicId));
        if (!account.getCompanyId().equals(companyId)) {
            throw DomainException.notFound("Account", accountPublicId);
        }
        return account;
    }

    /**
     * The JournalLine predicate that scopes a ledger/aggregation query to one account.
     *
     * <p>When {@code posting_type == HEADER} (a group/parent node), the predicate widens to every
     * POSTING descendant inside the account's nested-set window {@code [lft, rgt]} — mirrors the
     * reference's Celko interval-tree containment ({@code A.lft < D.lft < D.rgt < A.rgt}). When
     * {@code posting_type == POSTING}, {@code lft == rgt - 1} so this degenerates to the
     * single-account case without a separate code path.
     */
    public BooleanExpression accountScopePredicate(Account scope) {
        QJournalLine line = QJournalLine.journalLine;
        if (scope.getPostingType() == AccountPostingType.HEADER) {
            QAccount descendant = QAccount.account;
            var descendantIds = com.querydsl.jpa.JPAExpressions
                .select(descendant.publicId)
                .from(descendant)
                .where(descendant.companyId.eq(scope.getCompanyId())
                    .and(descendant.lft.gt(scope.getLft()))
                    .and(descendant.rgt.lt(scope.getRgt())));
            return line.accountId.in(descendantIds);
        }
        return line.accountId.eq(scope.getPublicId());
    }

    /** Validate an optional [fromDate, toDate] range: to must be on/after from when both are set. */
    public void validateDateRange(java.time.LocalDate fromDate, java.time.LocalDate toDate) {
        if (fromDate != null && toDate != null && toDate.isBefore(fromDate)) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "toDate must be on or after fromDate");
        }
    }
}

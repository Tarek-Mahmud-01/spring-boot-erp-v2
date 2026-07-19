package com.guru.erp.modules.finance.gl.service;

import com.guru.erp.modules.finance.gl.dto.JournalLineDtos.JournalLineRequest;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Enforces the two hard financial invariants shared by every write path (create, draft-line
 * replacement, reversal, system-voucher posting) — reference {@code views/journal_entries.py}
 * {@code _insert_journal_lines} / {@code create_journal_entry} and
 * {@code integrations/system_postings.py} {@code post_system_voucher}.
 *
 * <ol>
 *   <li><b>Per-line xor</b>: exactly one of {@code debit}/{@code credit} is strictly positive, the
 *       other exactly zero. Never both nonzero, never both zero. Also reproduced as the
 *       {@code ck_journal_lines_xor} DB check constraint in V71 — this service check exists so a
 *       violation surfaces as a clean {@link DomainException} instead of an opaque
 *       {@code DataIntegrityViolationException} from the DB.</li>
 *   <li><b>Balanced voucher</b>: {@code sum(debit) == sum(credit)} EXACTLY across all lines (in the
 *       amounts actually being compared — base-currency amounts for a multi-currency voucher).
 *       Never silently rounds; a one-cent imbalance throws.</li>
 * </ol>
 *
 * <p>Also requires &gt;= 2 lines (reference {@code JournalMinLinesError} — a single-line "journal
 * entry" cannot be double-entry by definition).
 */
@Service
public class BalancingService {

    private static final int MIN_LINES = 2;

    /** Validate a create/replace-lines request DTO before any entity is built. */
    public void assertBalancedRequest(List<JournalLineRequest> lines) {
        if (lines == null || lines.size() < MIN_LINES) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "A journal entry must have at least " + MIN_LINES + " lines");
        }
        long totalDebit = 0;
        long totalCredit = 0;
        for (JournalLineRequest ln : lines) {
            assertXor(ln.debit(), ln.credit());
            totalDebit += ln.debit();
            totalCredit += ln.credit();
        }
        assertBalanced(totalDebit, totalCredit);
    }

    /** Validate a single line's debit/credit xor invariant. */
    public void assertXor(long debit, long credit) {
        if (debit < 0 || credit < 0) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "debit and credit must both be >= 0");
        }
        boolean debitPositive = debit > 0;
        boolean creditPositive = credit > 0;
        if (debitPositive == creditPositive) {
            // both zero, or both positive — either way the line is invalid.
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Each journal line must have exactly one of debit or credit strictly positive "
                    + "(debit=" + debit + ", credit=" + credit + ")");
        }
    }

    /** Validate the debit/credit totals of an already-built voucher balance EXACTLY. */
    public void assertBalanced(long totalDebit, long totalCredit) {
        if (totalDebit != totalCredit) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Unbalanced voucher: total debit (" + totalDebit + ") must equal total credit ("
                    + totalCredit + ")");
        }
    }
}

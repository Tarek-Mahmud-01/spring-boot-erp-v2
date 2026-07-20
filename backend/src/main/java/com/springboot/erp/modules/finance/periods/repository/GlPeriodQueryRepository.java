package com.springboot.erp.modules.finance.periods.repository;

import com.springboot.erp.modules.finance.gl.domain.JournalEntry;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Read-only GL queries in support of the period-close checklist and BAS box computation.
 * {@code gl} and {@code periods} are both sub-slices of the same {@code finance} module, so a
 * direct repository query here is fine (reference {@code app.finance.views.period_close} /
 * {@code app.finance.views.bas} both import GL models directly) — this is NOT a cross-module
 * OutboxPublisher case; only crossing a module boundary (pos/inventory -&gt; finance) needs the
 * outbox.
 */
public interface GlPeriodQueryRepository extends JpaRepository<JournalEntry, Long> {

    /** FR-249 NO_DRAFT_JOURNALS check — DRAFT entries whose entryDate falls inside the period range. */
    @Query("""
        select e from JournalEntry e
        where e.companyId = :companyId
          and e.status = com.springboot.erp.modules.finance.gl.domain.JournalEntryStatus.DRAFT
          and e.entryDate >= :dateFrom and e.entryDate <= :dateTo
        """)
    List<JournalEntry> findDraftInRange(@Param("companyId") String companyId,
                                        @Param("dateFrom") LocalDate dateFrom,
                                        @Param("dateTo") LocalDate dateTo);

    /**
     * FR-249 TRIAL_BALANCE_BALANCED check — sum(baseDebit) / sum(baseCredit) across every POSTED
     * line up to {@code toDate} (reference {@code trial_balance_report_view.build_report}).
     */
    @Query("""
        select coalesce(sum(l.baseDebit), 0), coalesce(sum(l.baseCredit), 0)
        from JournalLine l join l.entry e
        where e.companyId = :companyId
          and e.status = com.springboot.erp.modules.finance.gl.domain.JournalEntryStatus.POSTED
          and e.entryDate <= :toDate
        """)
    List<Object[]> sumPostedDebitCredit(@Param("companyId") String companyId, @Param("toDate") LocalDate toDate);

    /**
     * BAS box computation — {voucherType, sum(baseDebit), sum(baseCredit)} for every POSTED line on
     * one account within a date range (reference {@code BasReportView._balances_by_voucher_type}).
     * Aggregating in base currency (never the raw transaction-currency debit/credit) ties the BAS
     * boxes to the Trial Balance the same way the reference does.
     */
    @Query("""
        select e.voucherType, coalesce(sum(l.baseDebit), 0), coalesce(sum(l.baseCredit), 0)
        from JournalLine l join l.entry e
        where e.companyId = :companyId
          and e.status = com.springboot.erp.modules.finance.gl.domain.JournalEntryStatus.POSTED
          and l.accountId = :accountId
          and e.entryDate >= :dateFrom and e.entryDate <= :dateTo
        group by e.voucherType
        """)
    List<Object[]> sumByVoucherTypeForAccount(@Param("companyId") String companyId,
                                              @Param("accountId") String accountId,
                                              @Param("dateFrom") LocalDate dateFrom,
                                              @Param("dateTo") LocalDate dateTo);
}

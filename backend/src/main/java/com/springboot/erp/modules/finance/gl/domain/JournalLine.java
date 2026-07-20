package com.springboot.erp.modules.finance.gl.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-046a JournalLine (reference {@code app.finance.models.JournalLine}).
 *
 * <p><b>HARD INVARIANT</b>: exactly one of {@code debit} / {@code credit} is strictly positive, the
 * other is exactly zero — {@code (debit = 0 AND credit > 0) OR (debit > 0 AND credit = 0)}. NEVER
 * both nonzero, NEVER both zero. Enforced twice: as a DB check constraint
 * ({@code ck_journal_lines_xor} in V71) AND in {@code BalancingService} before persisting — the
 * service check exists because a check constraint alone would surface as an opaque
 * {@code DataIntegrityViolationException} instead of a clean {@link com.springboot.erp.platform.error.DomainException}.
 *
 * <p>{@code accountId} is a loose cross-module ULID reference (a Chart-of-Accounts slice, not yet
 * ported) — no hard FK. {@code holderType}/{@code holderId} are the optional polymorphic subledger
 * pointer (supplier/customer/employee, FR-243) — also loose, since the holder lives in another
 * module's table. {@code currency} + {@code exchangeRate} + {@code baseDebit}/{@code baseCredit}
 * carry the FX trail for a multi-currency line: {@code exchangeRate} is the multiplier resolved by
 * the caller (1 when the line is already in the company's base currency), and
 * {@code baseDebit}/{@code baseCredit} are the converted amounts in company base currency — the
 * balanced-voucher check in {@code BalancingService} sums {@code baseDebit}/{@code baseCredit},
 * never the raw transaction-currency {@code debit}/{@code credit}, so a multi-currency voucher
 * balances in the ledger's home currency exactly like the reference.
 */
@Entity
@Table(name = "journal_lines")
public class JournalLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private JournalEntry entry;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "account_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "holder_type", nullable = false, length = 16)
    private HolderType holderType = HolderType.NONE;

    /** ULID of the supplier/customer/employee record the line is attributed to; null when {@code NONE}. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "holder_id", length = 26, columnDefinition = "char(26)")
    private String holderId;

    @Column(name = "narration", nullable = false, length = 500)
    private String narration = "";

    /** Transaction-currency debit (minor units). Exactly one of debit/credit > 0 — see class javadoc. */
    @Column(name = "debit", nullable = false)
    private long debit = 0L;

    /** Transaction-currency credit (minor units). Exactly one of debit/credit > 0 — see class javadoc. */
    @Column(name = "credit", nullable = false)
    private long credit = 0L;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency = "USD";

    /** Multiplier resolved at posting time; 1 when {@code currency} already equals the company base. */
    @Column(name = "exchange_rate", nullable = false, precision = 18, scale = 8)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    /** {@code debit} converted to company base currency; equals {@code debit} when rate = 1. */
    @Column(name = "base_debit", nullable = false)
    private long baseDebit = 0L;

    /** {@code credit} converted to company base currency; equals {@code credit} when rate = 1. */
    @Column(name = "base_credit", nullable = false)
    private long baseCredit = 0L;

    /**
     * Per-line branch override — lets a transfer voucher carry the destination branch on its DR
     * line and the source branch on its CR line, since the entry header's single {@code locationId}
     * only fits one of the two. Loose cross-module ULID; falls back to the header's location when
     * null (service-layer default, not a DB default).
     */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "location_id", length = 26, columnDefinition = "char(26)")
    private String locationId;

    public JournalLine() {
    }

    public JournalEntry getEntry() {
        return entry;
    }

    public void setEntry(JournalEntry entry) {
        this.entry = entry;
    }

    public int getLineNo() {
        return lineNo;
    }

    public void setLineNo(int lineNo) {
        this.lineNo = lineNo;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public HolderType getHolderType() {
        return holderType;
    }

    public void setHolderType(HolderType holderType) {
        this.holderType = holderType;
    }

    public String getHolderId() {
        return holderId;
    }

    public void setHolderId(String holderId) {
        this.holderId = holderId;
    }

    public String getNarration() {
        return narration;
    }

    public void setNarration(String narration) {
        this.narration = narration;
    }

    public long getDebit() {
        return debit;
    }

    public void setDebit(long debit) {
        this.debit = debit;
    }

    public long getCredit() {
        return credit;
    }

    public void setCredit(long credit) {
        this.credit = credit;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public long getBaseDebit() {
        return baseDebit;
    }

    public void setBaseDebit(long baseDebit) {
        this.baseDebit = baseDebit;
    }

    public long getBaseCredit() {
        return baseCredit;
    }

    public void setBaseCredit(long baseCredit) {
        this.baseCredit = baseCredit;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }
}

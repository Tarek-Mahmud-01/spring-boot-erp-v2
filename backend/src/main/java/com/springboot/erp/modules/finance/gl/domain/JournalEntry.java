package com.springboot.erp.modules.finance.gl.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-046 JournalEntry header (reference {@code app.finance.models.JournalEntry} +
 * {@code views/journal_entries.py}). Lifecycle DRAFT -&gt; POSTED -&gt; REVERSED, enforced by
 * {@link com.springboot.erp.platform.status.StateMachine} in the posting/reversal services.
 *
 * <p><b>Soft-delete note</b>: this entity extends {@link BaseEntity} like every other entity for
 * consistency, so its {@code @SQLRestriction("deleted_at is null")} hides soft-deleted rows from
 * every query automatically — matching the reference's own {@code deleted_at} column and its
 * "Audit C5" rule (a soft-deleted voucher must disappear from lists/get). This is safe here
 * because, unlike Account's nested-set tree, JournalEntry rows have no structural parent/child
 * relationship that a hidden row could corrupt; only DRAFT entries may ever be deleted (enforced
 * in the service), so a POSTED/REVERSED row is never soft-deleted and the "immutable once posted"
 * invariant is never at odds with the restriction.
 *
 * <p>{@code companyId} / {@code locationId} are loose cross-module ULID references (settings.company,
 * settings.location) — no hard FK, per the vertical-slice rule. {@code totalDebit} / {@code totalCredit}
 * are pre-aggregated (long minor units, in the company's base currency) so list views never need to
 * re-sum lines; the posting service is the single place that must keep them equal (the balanced-voucher
 * invariant) before a DRAFT may transition to POSTED.
 */
@Entity
@Table(name = "journal_entries")
public class JournalEntry extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "company_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String companyId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "location_id", length = 26, columnDefinition = "char(26)")
    private String locationId;

    @Column(name = "voucher_type", nullable = false, length = 10)
    private String voucherType;

    @Column(name = "voucher_number", nullable = false, length = 40)
    private String voucherNumber;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    /** {@code YYYY-MM} — derived from {@code entryDate}, drives period-close gating. */
    @Column(name = "period_code", nullable = false, length = 10)
    private String periodCode;

    @Column(name = "reference", length = 200)
    private String reference;

    @Column(name = "narration", nullable = false, length = 1000)
    private String narration = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private JournalEntryStatus status = JournalEntryStatus.DRAFT;

    /** The reversal entry that voided this one, if any. Loose same-table self-reference by public id. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "reversed_by_id", length = 26, columnDefinition = "char(26)")
    private String reversedById;

    /** Sum(lines.debit) in the company base currency — kept in lockstep with the lines by the service. */
    @Column(name = "total_debit", nullable = false)
    private long totalDebit = 0L;

    /** Sum(lines.credit) in the company base currency — MUST equal {@link #totalDebit} once POSTED. */
    @Column(name = "total_credit", nullable = false)
    private long totalCredit = 0L;

    @Column(name = "posted_at")
    private Instant postedAt;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "posted_by", length = 26, columnDefinition = "char(26)")
    private String postedBy;

    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo asc")
    private List<JournalLine> lines = new ArrayList<>();

    public JournalEntry() {
    }

    public void addLine(JournalLine line) {
        line.setEntry(this);
        lines.add(line);
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getVoucherType() {
        return voucherType;
    }

    public void setVoucherType(String voucherType) {
        this.voucherType = voucherType;
    }

    public String getVoucherNumber() {
        return voucherNumber;
    }

    public void setVoucherNumber(String voucherNumber) {
        this.voucherNumber = voucherNumber;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public String getPeriodCode() {
        return periodCode;
    }

    public void setPeriodCode(String periodCode) {
        this.periodCode = periodCode;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getNarration() {
        return narration;
    }

    public void setNarration(String narration) {
        this.narration = narration;
    }

    public JournalEntryStatus getStatus() {
        return status;
    }

    public void setStatus(JournalEntryStatus status) {
        this.status = status;
    }

    public String getReversedById() {
        return reversedById;
    }

    public void setReversedById(String reversedById) {
        this.reversedById = reversedById;
    }

    public long getTotalDebit() {
        return totalDebit;
    }

    public void setTotalDebit(long totalDebit) {
        this.totalDebit = totalDebit;
    }

    public long getTotalCredit() {
        return totalCredit;
    }

    public void setTotalCredit(long totalCredit) {
        this.totalCredit = totalCredit;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(Instant postedAt) {
        this.postedAt = postedAt;
    }

    public String getPostedBy() {
        return postedBy;
    }

    public void setPostedBy(String postedBy) {
        this.postedBy = postedBy;
    }

    public List<JournalLine> getLines() {
        return lines;
    }
}

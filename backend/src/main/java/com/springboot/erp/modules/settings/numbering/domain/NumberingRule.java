package com.springboot.erp.modules.settings.numbering.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-006 NumberingRule (US-005 / FR-022-025). One rule per (company, document
 * type) issues the running sequence numbers for that document kind.
 *
 * <p>{@code totalIssued} is a cumulative counter across resets used by FR-025 to
 * detect that a rule "has issued at least one number" regardless of cadence
 * resets (once locked, only {@code padding} may change, upward). {@code currentValue}
 * is the per-window counter that resets to {@code startValue - 1} whenever the
 * active {@code currentWindowKey} changes.
 *
 * <p>In this v2 slice the owning company is referenced by its public id (ULID)
 * rather than a JPA relation — there is no Company entity in v2 yet, and the
 * reference API exposes/accepts companies by public id anyway.
 *
 * <p>Constraints (mirrored in the Flyway migration V13):
 * <ul>
 *   <li>unique {@code (company_id, document_type)} — {@code uq_numbering_rules_company_doctype}</li>
 *   <li>check {@code document_type in (...)} — enforced by the enum + a DB check</li>
 *   <li>check {@code reset_cadence in (...)} — enforced by the enum + a DB check</li>
 *   <li>check {@code padding between 4 and 10}, {@code start_value >= 1}</li>
 * </ul>
 */
@Entity
@Table(
    name = "numbering_rules",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_numbering_rules_company_doctype",
        columnNames = {"company_id", "document_type"}))
public class NumberingRule extends BaseEntity {

    /** Owning company's public id (ULID). Immutable after creation. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "company_id", nullable = false, updatable = false, length = 26, columnDefinition = "char(26)")
    private String companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, updatable = false, length = 20)
    private DocumentType documentType;

    @Column(name = "prefix", nullable = false, length = 10)
    private String prefix = "";

    @Column(name = "padding", nullable = false)
    private int padding;

    @Enumerated(EnumType.STRING)
    @Column(name = "reset_cadence", nullable = false, length = 10)
    private ResetCadence resetCadence = ResetCadence.NEVER;

    @Column(name = "start_value", nullable = false)
    private long startValue = 1L;

    @Column(name = "current_value", nullable = false)
    private long currentValue = 0L;

    @Column(name = "current_window_key", nullable = false, length = 10)
    private String currentWindowKey = "";

    @Column(name = "total_issued", nullable = false)
    private long totalIssued = 0L;

    protected NumberingRule() {
    }

    /** Create a fresh rule; {@code currentValue} starts one below {@code startValue}. */
    public static NumberingRule create(String companyId, DocumentType documentType, String prefix,
                                       int padding, ResetCadence resetCadence, long startValue) {
        NumberingRule r = new NumberingRule();
        r.companyId = companyId;
        r.documentType = documentType;
        r.prefix = prefix == null ? "" : prefix;
        r.padding = padding;
        r.resetCadence = resetCadence == null ? ResetCadence.NEVER : resetCadence;
        r.startValue = startValue;
        r.currentValue = startValue - 1;
        r.currentWindowKey = "";
        r.totalIssued = 0L;
        return r;
    }

    /** FR-025: a rule that has already issued a number is locked (see service rules). */
    public boolean isLocked() {
        return totalIssued > 0;
    }

    public void changePrefix(String prefix) {
        this.prefix = prefix == null ? "" : prefix;
    }

    public void changePadding(int padding) {
        this.padding = padding;
    }

    public void changeResetCadence(ResetCadence resetCadence) {
        this.resetCadence = resetCadence;
    }

    /** Resetting the start value rewinds the per-window counter and clears the window. */
    public void changeStartValue(long startValue) {
        this.startValue = startValue;
        this.currentValue = startValue - 1;
        this.currentWindowKey = "";
    }

    /**
     * FR-023/FR-024: advance the sequence for {@code windowKey}. If the window
     * changed, the counter rewinds to {@code startValue - 1} first. Returns the
     * new (allocated) value.
     */
    public long advance(String windowKey) {
        if (!windowKey.equals(currentWindowKey)) {
            currentWindowKey = windowKey;
            currentValue = startValue - 1;
        }
        currentValue += 1;
        totalIssued += 1;
        return currentValue;
    }

    /** AC-005-1: {@code PREFIX-WINDOW-0000} — joins non-empty parts with '-'. */
    public String formatNumber(String windowKey, long value) {
        StringBuilder sb = new StringBuilder();
        if (prefix != null && !prefix.isEmpty()) {
            String p = prefix;
            while (p.endsWith("-")) {
                p = p.substring(0, p.length() - 1);
            }
            sb.append(p);
        }
        if (windowKey != null && !windowKey.isEmpty()) {
            if (sb.length() > 0) {
                sb.append('-');
            }
            sb.append(windowKey);
        }
        if (sb.length() > 0) {
            sb.append('-');
        }
        sb.append(padValue(value));
        return sb.toString();
    }

    private String padValue(long value) {
        String digits = Long.toString(value);
        int pad = padding - digits.length();
        if (pad <= 0) {
            return digits;
        }
        return "0".repeat(pad) + digits;
    }

    /**
     * FR-022/AC-005-4: the window key that contains {@code ref} for this rule's
     * cadence. NEVER → empty, YEARLY → the fiscal year YYYY, MONTHLY → YYYY-MM.
     *
     * @param fiscalYearStart the company's fiscal year start as {@code MM-DD}
     */
    public String windowKeyFor(String fiscalYearStart, LocalDate ref) {
        return switch (resetCadence) {
            case NEVER -> "";
            case YEARLY -> fiscalYearWindowKey(fiscalYearStart, ref);
            case MONTHLY -> "%04d-%02d".formatted(ref.getYear(), ref.getMonthValue());
        };
    }

    private static String fiscalYearWindowKey(String fiscalYearStart, LocalDate ref) {
        int month = Integer.parseInt(fiscalYearStart.split("-")[0]);
        if (ref.getMonthValue() >= month) {
            return "%04d".formatted(ref.getYear());
        }
        return "%04d".formatted(ref.getYear() - 1);
    }

    public String getCompanyId() {
        return companyId;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getPadding() {
        return padding;
    }

    public ResetCadence getResetCadence() {
        return resetCadence;
    }

    public long getStartValue() {
        return startValue;
    }

    public long getCurrentValue() {
        return currentValue;
    }

    public String getCurrentWindowKey() {
        return currentWindowKey;
    }

    public long getTotalIssued() {
        return totalIssued;
    }
}

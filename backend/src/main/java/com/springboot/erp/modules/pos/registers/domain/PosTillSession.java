package com.springboot.erp.modules.pos.registers.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import com.springboot.erp.platform.money.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * PosTillSession — a cashier's till shift on a {@link Register} (US-037 /
 * FR-191..194). Opens with an opening cash float, accrues cash movements
 * ({@link PosTillMovement}), and closes with a counted-cash reconciliation
 * (variance over the tenant threshold requires manager sign-off, recorded via
 * {@code varianceApprovedBy}/{@code varianceApprovedAt}).
 *
 * <p>Domain columns only; base columns from {@link BaseEntity}. Cross-slice
 * references (the shift's cashier user, the approving manager) are loose ULID
 * {@code char(26)} columns. {@code register} IS same-slice, so it keeps a real
 * FK ({@code on delete restrict} — a register with till history cannot be hard
 * deleted). {@link PosTillMovement} is a same-slice child with cascade.
 *
 * <p>Constraints reproduced in V50: status check, and a partial unique index
 * enforcing at most one {@code OPEN} session per register at a time (reference
 * {@code uq_till_one_open_per_register}).
 */
@Entity
@Table(name = "pos_till_sessions")
public class PosTillSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "register_id", nullable = false)
    private Register register;

    /** ULID public id of the Location (cross-slice, denormalised from the register at open time). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "location_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String locationId;

    /** ULID public id of the operator who opened the shift (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cashier_id", length = 26, columnDefinition = "char(26)")
    private String cashierId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 8)
    private TillSessionStatus status = TillSessionStatus.OPEN;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "opening_float_amount", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "opening_float_currency", nullable = false, length = 3, columnDefinition = "char(3)"))
    })
    private Money openingFloat;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "expected_cash_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "expected_cash_currency", length = 3, columnDefinition = "char(3)"))
    })
    private Money expectedCash;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "counted_cash_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "counted_cash_currency", length = 3, columnDefinition = "char(3)"))
    })
    private Money countedCash;

    /** Signed: counted - expected. Null until closed. */
    @Column(name = "variance_amount")
    private Long varianceAmount;

    /** ULID public id of the manager who approved an over-threshold variance (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "variance_approved_by", length = 26, columnDefinition = "char(26)")
    private String varianceApprovedBy;

    @Column(name = "variance_approved_at")
    private Instant varianceApprovedAt;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt asc, id asc")
    private List<PosTillMovement> movements = new ArrayList<>();

    public PosTillSession() {
    }

    public void addMovement(PosTillMovement movement) {
        movement.setSession(this);
        movements.add(movement);
    }

    public Register getRegister() {
        return register;
    }

    public void setRegister(Register register) {
        this.register = register;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getCashierId() {
        return cashierId;
    }

    public void setCashierId(String cashierId) {
        this.cashierId = cashierId;
    }

    public TillSessionStatus getStatus() {
        return status;
    }

    public void setStatus(TillSessionStatus status) {
        this.status = status;
    }

    public Money getOpeningFloat() {
        return openingFloat;
    }

    public void setOpeningFloat(Money openingFloat) {
        this.openingFloat = openingFloat;
    }

    public Money getExpectedCash() {
        return expectedCash;
    }

    public void setExpectedCash(Money expectedCash) {
        this.expectedCash = expectedCash;
    }

    public Money getCountedCash() {
        return countedCash;
    }

    public void setCountedCash(Money countedCash) {
        this.countedCash = countedCash;
    }

    public Long getVarianceAmount() {
        return varianceAmount;
    }

    public void setVarianceAmount(Long varianceAmount) {
        this.varianceAmount = varianceAmount;
    }

    public String getVarianceApprovedBy() {
        return varianceApprovedBy;
    }

    public void setVarianceApprovedBy(String varianceApprovedBy) {
        this.varianceApprovedBy = varianceApprovedBy;
    }

    public Instant getVarianceApprovedAt() {
        return varianceApprovedAt;
    }

    public void setVarianceApprovedAt(Instant varianceApprovedAt) {
        this.varianceApprovedAt = varianceApprovedAt;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public List<PosTillMovement> getMovements() {
        return movements;
    }
}

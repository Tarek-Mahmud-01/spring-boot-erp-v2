package com.springboot.erp.modules.procurement.suppliers.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import com.springboot.erp.platform.money.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-026 Supplier — the AP master (reference {@code app.procurement.models.Supplier}, US-016 /
 * FR-075–079). Carries contact / address / bank details as free-form {@code jsonb} maps, a trade
 * {@link SupplierType}, a {@link SupplierStatus} lifecycle, credit limit + opening balance as
 * {@link Money}, and an optional AU ABN.
 *
 * <p>Domain columns only; base columns (public_id, created/updated at+by, deleted_at, version) come
 * from {@link BaseEntity}. Cross-slice references (location, opening-balance GL account) are held as
 * loose ULID {@code char(26)} columns — no hard cross-slice FK, per the vertical-slice rule.
 * Constraints reproduced in V40: unique {@code code}, {@code ck_suppliers_type} /
 * {@code ck_suppliers_status} / {@code ck_suppliers_ob_side} checks.
 */
@Entity
@Table(name = "suppliers")
public class Supplier extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private SupplierType type = SupplierType.BOTH;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contact", columnDefinition = "jsonb")
    private Map<String, Object> contact;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "address", columnDefinition = "jsonb")
    private Map<String, Object> address;

    @Column(name = "payment_terms", length = 100)
    private String paymentTerms;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "default_currency", nullable = false, length = 3, columnDefinition = "char(3)")
    private String defaultCurrency = "USD";

    @Column(name = "tax_registration_no", length = 100)
    private String taxRegistrationNo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bank_details", columnDefinition = "jsonb")
    private Map<String, Object> bankDetails;

    /** Credit limit — minor units + currency (reference credit_limit_amount / _currency). */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "credit_limit_amount", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "credit_limit_currency", nullable = false, length = 3, columnDefinition = "char(3)"))
    })
    private Money creditLimit = Money.zero("USD");

    @Convert(converter = SupplierStatusConverter.class)
    @Column(name = "status", nullable = false, length = 10)
    private SupplierStatus status = SupplierStatus.ACTIVE;

    @Column(name = "block_reason", length = 500)
    private String blockReason;

    /** Australian Business Number (11 digits, ATO checksum); optional for non-AU suppliers. */
    @Column(name = "abn", length = 11)
    private String abn;

    /** ULID public id of the associated Location (cross-slice, resolved app-side; no hard FK). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "location_id", length = 26, columnDefinition = "char(26)")
    private String locationId;

    /** Opening AP balance — minor units + currency (reference opening_balance_amount / _currency). */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "opening_balance_amount", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "opening_balance_currency", nullable = false, length = 3, columnDefinition = "char(3)"))
    })
    private Money openingBalance = Money.zero("USD");

    /** DEBIT | CREDIT — which side the opening balance sits on (ck_suppliers_ob_side). */
    @Column(name = "opening_balance_side", nullable = false, length = 6)
    private String openingBalanceSide = "CREDIT";

    @Column(name = "opening_balance_date")
    private LocalDate openingBalanceDate;

    /** Rate to convert opening-balance currency → company base currency (null = 1.0). */
    @Column(name = "opening_balance_exchange_rate", precision = 18, scale = 6)
    private BigDecimal openingBalanceExchangeRate;

    /** ULID public id of the GL contra account for the opening balance (cross-slice, loose ref). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "opening_balance_account_id", length = 26, columnDefinition = "char(26)")
    private String openingBalanceAccountId;

    public Supplier() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SupplierType getType() {
        return type;
    }

    public void setType(SupplierType type) {
        this.type = type;
    }

    public Map<String, Object> getContact() {
        return contact;
    }

    public void setContact(Map<String, Object> contact) {
        this.contact = contact;
    }

    public Map<String, Object> getAddress() {
        return address;
    }

    public void setAddress(Map<String, Object> address) {
        this.address = address;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(String paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    public String getTaxRegistrationNo() {
        return taxRegistrationNo;
    }

    public void setTaxRegistrationNo(String taxRegistrationNo) {
        this.taxRegistrationNo = taxRegistrationNo;
    }

    public Map<String, Object> getBankDetails() {
        return bankDetails;
    }

    public void setBankDetails(Map<String, Object> bankDetails) {
        this.bankDetails = bankDetails;
    }

    public Money getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(Money creditLimit) {
        this.creditLimit = creditLimit;
    }

    public SupplierStatus getStatus() {
        return status;
    }

    public void setStatus(SupplierStatus status) {
        this.status = status;
    }

    public String getBlockReason() {
        return blockReason;
    }

    public void setBlockReason(String blockReason) {
        this.blockReason = blockReason;
    }

    public String getAbn() {
        return abn;
    }

    public void setAbn(String abn) {
        this.abn = abn;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public Money getOpeningBalance() {
        return openingBalance;
    }

    public void setOpeningBalance(Money openingBalance) {
        this.openingBalance = openingBalance;
    }

    public String getOpeningBalanceSide() {
        return openingBalanceSide;
    }

    public void setOpeningBalanceSide(String openingBalanceSide) {
        this.openingBalanceSide = openingBalanceSide;
    }

    public LocalDate getOpeningBalanceDate() {
        return openingBalanceDate;
    }

    public void setOpeningBalanceDate(LocalDate openingBalanceDate) {
        this.openingBalanceDate = openingBalanceDate;
    }

    public BigDecimal getOpeningBalanceExchangeRate() {
        return openingBalanceExchangeRate;
    }

    public void setOpeningBalanceExchangeRate(BigDecimal openingBalanceExchangeRate) {
        this.openingBalanceExchangeRate = openingBalanceExchangeRate;
    }

    public String getOpeningBalanceAccountId() {
        return openingBalanceAccountId;
    }

    public void setOpeningBalanceAccountId(String openingBalanceAccountId) {
        this.openingBalanceAccountId = openingBalanceAccountId;
    }
}

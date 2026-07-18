package com.guru.erp.modules.settings.company.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-001 Company — the tenant root of a single-tenant deployment.
 *
 * <p>Domain columns only; id / publicId / audit / version / soft-delete all come
 * from {@link BaseEntity}. Constraints reproduced from the reference model
 * (see V14__company.sql for the matching DDL):
 * <ul>
 *   <li>{@code unique(code)} — FR-001, one company code per tenant.</li>
 *   <li>Partial unique on {@code is_primary = true} — FR-004, exactly one primary.</li>
 *   <li>Partial unique on {@code abn is not null} — FR-AU-001, ABN uniqueness.</li>
 *   <li>Check constraints on status / compliance_profile / bas_period enum values.</li>
 * </ul>
 * {@code base_currency} is immutable after creation (enforced in the service).
 */
@Entity
@Table(name = "companies")
public class Company extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "legal_name", nullable = false, length = 200)
    private String legalName;

    @Column(name = "trading_name", length = 200)
    private String tradingName;

    @Column(name = "country", nullable = false, length = 2)
    private String country;

    @Column(name = "base_currency", nullable = false, updatable = false, length = 3)
    private String baseCurrency;

    @Column(name = "tax_registration_no", length = 30)
    private String taxRegistrationNo;

    @Column(name = "tax_registered", nullable = false)
    private boolean taxRegistered = false;

    @Column(name = "tax_registration_date")
    private LocalDate taxRegistrationDate;

    @Column(name = "fiscal_year_start", nullable = false, length = 5)
    private String fiscalYearStart;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    // Persisted via CompanyStatusConverter as the lower-case wire value
    // ('active'/'inactive') to satisfy ck_companies_status; @Enumerated(STRING)
    // would store the uppercase constant name and violate the check constraint.
    @Convert(converter = CompanyStatusConverter.class)
    @Column(name = "status", nullable = false, length = 16)
    private CompanyStatus status = CompanyStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_profile", nullable = false, length = 16)
    private ComplianceProfile complianceProfile = ComplianceProfile.NONE;

    @Column(name = "abn", length = 11)
    private String abn;

    @Column(name = "acn", length = 9)
    private String acn;

    @Column(name = "gst_registration_date")
    private LocalDate gstRegistrationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "bas_period", length = 16)
    private BasPeriod basPeriod;

    // Accepts an external URL or an inline data:image/...;base64 payload that can
    // run tens of KB, so the column is unbounded text (no length cap).
    @Column(name = "logo_url", columnDefinition = "text")
    private String logoUrl;

    // Free-form invoice header/footer layout owned by the frontend designer;
    // stored opaquely, no server-side structural validation.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "invoice_layout", columnDefinition = "jsonb")
    private Map<String, Object> invoiceLayout;

    public Company() {
    }

    // --- status maps 1:1 to the persisted enum; exposed for the derived DTO field ---

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getTradingName() {
        return tradingName;
    }

    public void setTradingName(String tradingName) {
        this.tradingName = tradingName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public String getTaxRegistrationNo() {
        return taxRegistrationNo;
    }

    public void setTaxRegistrationNo(String taxRegistrationNo) {
        this.taxRegistrationNo = taxRegistrationNo;
    }

    public boolean isTaxRegistered() {
        return taxRegistered;
    }

    public void setTaxRegistered(boolean taxRegistered) {
        this.taxRegistered = taxRegistered;
    }

    public LocalDate getTaxRegistrationDate() {
        return taxRegistrationDate;
    }

    public void setTaxRegistrationDate(LocalDate taxRegistrationDate) {
        this.taxRegistrationDate = taxRegistrationDate;
    }

    public String getFiscalYearStart() {
        return fiscalYearStart;
    }

    public void setFiscalYearStart(String fiscalYearStart) {
        this.fiscalYearStart = fiscalYearStart;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public CompanyStatus getStatus() {
        return status;
    }

    public void setStatus(CompanyStatus status) {
        this.status = status;
    }

    public ComplianceProfile getComplianceProfile() {
        return complianceProfile;
    }

    public void setComplianceProfile(ComplianceProfile complianceProfile) {
        this.complianceProfile = complianceProfile;
    }

    public String getAbn() {
        return abn;
    }

    public void setAbn(String abn) {
        this.abn = abn;
    }

    public String getAcn() {
        return acn;
    }

    public void setAcn(String acn) {
        this.acn = acn;
    }

    public LocalDate getGstRegistrationDate() {
        return gstRegistrationDate;
    }

    public void setGstRegistrationDate(LocalDate gstRegistrationDate) {
        this.gstRegistrationDate = gstRegistrationDate;
    }

    public BasPeriod getBasPeriod() {
        return basPeriod;
    }

    public void setBasPeriod(BasPeriod basPeriod) {
        this.basPeriod = basPeriod;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public Map<String, Object> getInvoiceLayout() {
        return invoiceLayout;
    }

    public void setInvoiceLayout(Map<String, Object> invoiceLayout) {
        this.invoiceLayout = invoiceLayout;
    }
}

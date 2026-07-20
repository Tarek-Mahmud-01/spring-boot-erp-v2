package com.springboot.erp.modules.settings.company.service;

import com.springboot.erp.modules.settings.company.domain.Company;
import com.springboot.erp.modules.settings.company.domain.CompanyStatus;
import com.springboot.erp.modules.settings.company.domain.CompanyValidators;
import com.springboot.erp.modules.settings.company.domain.ComplianceProfile;
import com.springboot.erp.modules.settings.company.dto.CompanyDtos.CompanyCreateRequest;
import com.springboot.erp.modules.settings.company.dto.CompanyDtos.CompanyResponse;
import com.springboot.erp.modules.settings.company.dto.CompanyDtos.CompanyUpdateRequest;
import com.springboot.erp.modules.settings.company.mapper.CompanyMapper;
import com.springboot.erp.modules.settings.company.repository.CompanyRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side use-cases for ENT-001 Company (ARCHITECTURE.md §2 — service holds
 * business rules). Ports the reference create/patch/delete/status flows: format
 * validation, single-tenant + duplicate-code guards, the AU cross-field rules,
 * the single-primary invariant, optimistic locking, soft delete, and one audit
 * row per mutation.
 *
 * <p>The reference additionally generated fiscal periods and emitted outbox
 * events on each mutation; those cross module boundaries and are intentionally
 * out of scope for this slice (see returned notes).
 */
@Service
public class CompanyCommandService {

    private static final String AUDIT_ENTITY = "company";

    private final CompanyRepository repository;
    private final CompanyMapper mapper;
    private final AuditService auditService;

    public CompanyCommandService(CompanyRepository repository, CompanyMapper mapper,
                                 AuditService auditService) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional
    public CompanyResponse create(CompanyCreateRequest req) {
        // Single-tenant deployment guard: at most one live company. Checked first
        // so the operator sees "already exists" rather than a duplicate-code error.
        if (repository.count() > 0) {
            throw new DomainException(ErrorCode.CONFLICT,
                "This deployment is single-tenant — a company already exists. "
                    + "Edit the existing company instead of creating a new one.");
        }

        String code = upper(req.code());
        if (!CompanyValidators.isValidCompanyCode(code)) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "code must be 3-10 uppercase alphanumeric characters");
        }
        if (repository.existsByCode(code)) {
            throw new DomainException(ErrorCode.DUPLICATE, "Company code already exists: " + code);
        }
        validateTaxNo(req.taxRegistrationNo());
        validateFiscalYearStart(req.fiscalYearStart());

        ComplianceProfile profile = req.complianceProfile() == null
            ? ComplianceProfile.NONE : req.complianceProfile();
        String abnClean = blankToNull(CompanyValidators.normaliseAbn(req.abn()));
        validateCrossFields(req.taxRegistered(), req.taxRegistrationDate(), profile,
            abnClean, req.gstRegistrationDate());
        if (abnClean != null && repository.existsByAbn(abnClean)) {
            throw new DomainException(ErrorCode.DUPLICATE, "ABN already registered: " + abnClean);
        }

        Company company = build(req, code, profile, abnClean);
        // FR-004: the first company is forced primary so the tenant always has one.
        company.setPrimary(true);

        Company saved = repository.save(company);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public CompanyResponse update(String publicId, CompanyUpdateRequest req) {
        Company company = load(publicId);
        checkVersion(company, req.version());
        CompanyResponse before = mapper.toResponse(company);

        // Proposed post-update state for cross-field validation (fall back to
        // current value when a field is omitted — PATCH semantics).
        boolean taxRegistered = req.taxRegistered() != null ? req.taxRegistered() : company.isTaxRegistered();
        var taxDate = req.taxRegistrationDate() != null ? req.taxRegistrationDate() : company.getTaxRegistrationDate();
        ComplianceProfile profile = req.complianceProfile() != null ? req.complianceProfile() : company.getComplianceProfile();
        String abn = req.abn() != null ? blankToNull(CompanyValidators.normaliseAbn(req.abn())) : company.getAbn();
        var gstDate = req.gstRegistrationDate() != null ? req.gstRegistrationDate() : company.getGstRegistrationDate();

        if (req.taxRegistrationNo() != null) {
            validateTaxNo(req.taxRegistrationNo());
        }
        if (req.fiscalYearStart() != null) {
            validateFiscalYearStart(req.fiscalYearStart());
        }
        validateCrossFields(taxRegistered, taxDate, profile, abn, gstDate);
        if (req.abn() != null && abn != null && !abn.equals(before.abn()) && repository.existsByAbn(abn)) {
            throw new DomainException(ErrorCode.DUPLICATE, "ABN already registered: " + abn);
        }

        applyPatch(company, req, abn);
        Company saved = repository.save(company);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(String publicId) {
        Company company = load(publicId);
        // FR-005 / AC-001-5: refuse to remove the sole primary — a tenant must
        // always keep exactly one primary company.
        if (company.isPrimary() && repository.countByPrimaryTrue() <= 1) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Cannot delete the primary company — set another company primary first");
        }
        CompanyResponse before = mapper.toResponse(company);
        company.softDelete();
        repository.save(company);
        auditService.record(AUDIT_ENTITY, publicId, AuditAction.DELETE, before, null);
    }

    @Transactional
    public CompanyResponse deactivate(String publicId) {
        Company company = load(publicId);
        // FR-005 / AC-001-4: the sole primary cannot be deactivated.
        if (company.isPrimary() && repository.countByPrimaryTrue() <= 1) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Cannot deactivate the primary company");
        }
        return changeStatus(company, CompanyStatus.INACTIVE);
    }

    @Transactional
    public CompanyResponse activate(String publicId) {
        return changeStatus(load(publicId), CompanyStatus.ACTIVE);
    }

    // --- helpers -----------------------------------------------------------

    private CompanyResponse changeStatus(Company company, CompanyStatus target) {
        CompanyResponse before = mapper.toResponse(company);
        company.setStatus(target);
        Company saved = repository.save(company);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    private Company load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("Company", publicId));
    }

    private void checkVersion(Company company, Long requestVersion) {
        if (requestVersion != null && requestVersion != company.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK,
                ErrorCode.OPTIMISTIC_LOCK.defaultDetail());
        }
    }

    private Company build(CompanyCreateRequest req, String code, ComplianceProfile profile, String abnClean) {
        Company c = new Company();
        c.setCode(code);
        c.setLegalName(req.legalName().strip());
        c.setTradingName(blankToNull(req.tradingName()));
        c.setCountry(upper(req.country()));
        c.setBaseCurrency(upper(req.baseCurrency()));
        c.setTaxRegistrationNo(req.taxRegistrationNo());
        c.setTaxRegistered(req.taxRegistered());
        c.setTaxRegistrationDate(req.taxRegistrationDate());
        c.setFiscalYearStart(req.fiscalYearStart());
        c.setStatus(CompanyStatus.ACTIVE);
        c.setComplianceProfile(profile);
        c.setAbn(abnClean);
        c.setAcn(blankToNull(CompanyValidators.normaliseAcn(req.acn())));
        c.setGstRegistrationDate(req.gstRegistrationDate());
        c.setBasPeriod(req.basPeriod());
        c.setLogoUrl(req.logoUrl());
        c.setInvoiceLayout(req.invoiceLayout());
        return c;
    }

    private void applyPatch(Company c, CompanyUpdateRequest req, String abn) {
        if (req.legalName() != null) {
            c.setLegalName(req.legalName().strip());
        }
        if (req.tradingName() != null) {
            c.setTradingName(blankToNull(req.tradingName()));
        }
        if (req.country() != null) {
            c.setCountry(upper(req.country()));
        }
        if (req.taxRegistrationNo() != null) {
            c.setTaxRegistrationNo(req.taxRegistrationNo());
        }
        if (req.taxRegistered() != null) {
            c.setTaxRegistered(req.taxRegistered());
        }
        if (req.taxRegistrationDate() != null) {
            c.setTaxRegistrationDate(req.taxRegistrationDate());
        }
        if (req.fiscalYearStart() != null) {
            c.setFiscalYearStart(req.fiscalYearStart());
        }
        if (req.complianceProfile() != null) {
            c.setComplianceProfile(req.complianceProfile());
        }
        if (req.abn() != null) {
            c.setAbn(abn);
        }
        if (req.acn() != null) {
            c.setAcn(blankToNull(CompanyValidators.normaliseAcn(req.acn())));
        }
        if (req.gstRegistrationDate() != null) {
            c.setGstRegistrationDate(req.gstRegistrationDate());
        }
        if (req.basPeriod() != null) {
            c.setBasPeriod(req.basPeriod());
        }
        if (req.logoUrl() != null) {
            c.setLogoUrl(req.logoUrl());
        }
        if (req.invoiceLayout() != null) {
            // Empty map clears the layout; null (omitted) means "no change".
            c.setInvoiceLayout(req.invoiceLayout());
        }
    }

    private void validateCrossFields(boolean taxRegistered, java.time.LocalDate taxDate,
                                     ComplianceProfile profile, String abn,
                                     java.time.LocalDate gstDate) {
        // FR-002 / AC-001-3 — registration date mandatory when the flag is true.
        if (taxRegistered && taxDate == null) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "tax_registration_date is required when tax_registered is true");
        }
        if (profile == ComplianceProfile.AU) {
            // FR-AU-001 — AU profile requires a valid ABN.
            if (abn == null || !CompanyValidators.isValidAbn(abn)) {
                throw new DomainException(ErrorCode.VALIDATION_FAILED,
                    "AU compliance profile requires a valid ABN");
            }
            // FR-AU-003 — AU + tax_registered also requires a GST registration date.
            if (taxRegistered && gstDate == null) {
                throw new DomainException(ErrorCode.VALIDATION_FAILED,
                    "gst_registration_date is required for AU tax-registered companies");
            }
        }
    }

    private void validateTaxNo(String value) {
        if (value != null && !CompanyValidators.isValidTaxRegistrationNo(value)) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "tax_registration_no must be 5-30 alphanumeric/format characters");
        }
    }

    private void validateFiscalYearStart(String value) {
        if (!CompanyValidators.isValidFiscalYearStart(value)) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "fiscal_year_start must be a valid 'MM-DD' calendar date");
        }
    }

    private static String upper(String value) {
        return value == null ? null : value.toUpperCase(java.util.Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}

package com.springboot.erp.modules.crm.customers.service;

import com.springboot.erp.modules.crm.customers.domain.ConsentChannel;
import com.springboot.erp.modules.crm.customers.domain.ConsentLog;
import com.springboot.erp.modules.crm.customers.domain.Customer;
import com.springboot.erp.modules.crm.customers.domain.CustomerProfile;
import com.springboot.erp.modules.crm.customers.domain.CustomerStatus;
import com.springboot.erp.modules.crm.customers.domain.CustomerType;
import com.springboot.erp.modules.crm.customers.dto.CustomerDtos.CustomerCreateRequest;
import com.springboot.erp.modules.crm.customers.dto.CustomerDtos.CustomerResponse;
import com.springboot.erp.modules.crm.customers.dto.CustomerDtos.CustomerUpdateRequest;
import com.springboot.erp.modules.crm.customers.mapper.CustomersMapper;
import com.springboot.erp.modules.crm.customers.repository.ConsentLogRepository;
import com.springboot.erp.modules.crm.customers.repository.CustomerRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.outbox.OutboxPublisher;
import com.springboot.erp.platform.security.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side use-cases for ENT-050 Customer + ENT-051 CustomerProfile
 * (US-038 / FR-199-204): create / update / hard-delete. Ports the reference
 * flows: contact-required + contact-unique validation (FR-200), initial
 * per-channel consent capture with a {@link ConsentLog} audit row on create
 * (FR-201), and a history-gated hard delete (FR-204). Consent-toggle and
 * anonymize flows live in {@link CustomerConsentService}. One audit row +
 * outbox event per mutation.
 */
@Service
public class CustomerCommandService {

    static final String AUDIT_CUSTOMER = "crm.customer";
    static final String EVENT_CUSTOMER_CREATED = "crm.customer.created";
    static final String EVENT_CUSTOMER_UPDATED = "crm.customer.updated";

    private final CustomerRepository customerRepository;
    private final ConsentLogRepository consentLogRepository;
    private final CustomersMapper mapper;
    private final AuditService auditService;
    private final OutboxPublisher outbox;
    private final CurrentUser currentUser;
    private final CustomerHistoryGuard historyGuard;
    private final MembershipIdGenerator membershipIdGenerator;

    public CustomerCommandService(CustomerRepository customerRepository,
                                  ConsentLogRepository consentLogRepository,
                                  CustomersMapper mapper, AuditService auditService,
                                  OutboxPublisher outbox, CurrentUser currentUser,
                                  CustomerHistoryGuard historyGuard,
                                  MembershipIdGenerator membershipIdGenerator) {
        this.customerRepository = customerRepository;
        this.consentLogRepository = consentLogRepository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.outbox = outbox;
        this.currentUser = currentUser;
        this.historyGuard = historyGuard;
        this.membershipIdGenerator = membershipIdGenerator;
    }

    /** FR-199/200/201 — create a customer + its 1:1 profile; consent starts off unless opted in. */
    @Transactional
    public CustomerResponse create(CustomerCreateRequest req) {
        String mobile = blank(req.mobile());
        String email = normalizeEmail(req.email());
        if (mobile == null && email == null) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "Customer requires a mobile or an email");
        }
        assertContactUnique(mobile, email, null);

        Instant now = Instant.now(Clock.systemUTC());
        String actor = currentUser.optional().map(p -> p.userPublicId()).orElse(null);

        Customer c = new Customer();
        c.setCompanyId(req.companyId());
        c.setMembershipId(membershipIdGenerator.next());
        c.setFirstName(req.firstName().strip());
        c.setLastName(req.lastName().strip());
        c.setType(req.type() == null ? CustomerType.INDIVIDUAL : CustomerType.valueOf(req.type()));
        c.setStatus(CustomerStatus.ACTIVE);
        c.setAnalyticsConsent(req.analyticsConsent());
        c.setPhotoUrl(req.photoUrl());

        CustomerProfile profile = new CustomerProfile();
        profile.setMobile(mobile);
        profile.setEmail(email);
        profile.setPostcode(blank(req.postcode()));
        profile.setDateOfBirth(req.dateOfBirth());
        profile.setPreferredLocationId(req.preferredLocationId());
        profile.setConsentEmail(req.emailConsent());
        profile.setConsentSms(req.smsConsent());
        profile.setConsentEmailAt(req.emailConsent() ? now : null);
        profile.setConsentSmsAt(req.smsConsent() ? now : null);
        profile.setConsentEmailBy(req.emailConsent() ? actor : null);
        profile.setConsentSmsBy(req.smsConsent() ? actor : null);
        c.setProfile(profile);

        Customer saved = customerRepository.save(c);

        if (req.emailConsent()) {
            appendConsentLog(saved, ConsentChannel.EMAIL, true, now, actor);
        }
        if (req.smsConsent()) {
            appendConsentLog(saved, ConsentChannel.SMS, true, now, actor);
        }

        CustomerResponse after = mapper.toResponse(saved);
        auditService.record(AUDIT_CUSTOMER, saved.getPublicId(), AuditAction.CREATE, null, after);
        outbox.publish(AUDIT_CUSTOMER, saved.getPublicId(), EVENT_CUSTOMER_CREATED, after);
        return after;
    }

    /** FR-200 — partial update; contact fields re-validated for required/unique on change. */
    @Transactional
    public CustomerResponse update(String publicId, CustomerUpdateRequest req) {
        Customer c = load(publicId);
        if (c.isAnonymized()) {
            throw new DomainException(ErrorCode.CONFLICT, "Customer '" + publicId + "' is anonymized and read-only");
        }
        checkVersion(c, req.version());
        CustomerProfile p = c.getProfile();
        CustomerResponse before = mapper.toResponse(c);

        String newMobile = p == null ? null : p.getMobile();
        String newEmail = p == null ? null : p.getEmail();
        if (req.mobile() != null) {
            newMobile = blank(req.mobile());
        }
        if (req.email() != null) {
            newEmail = normalizeEmail(req.email());
        }
        if (newMobile == null && newEmail == null) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "Customer requires a mobile or an email");
        }
        assertContactUnique(newMobile, newEmail, c.getId());

        if (req.firstName() != null) {
            c.setFirstName(req.firstName().strip());
        }
        if (req.lastName() != null) {
            c.setLastName(req.lastName().strip());
        }
        if (req.analyticsConsent() != null) {
            c.setAnalyticsConsent(req.analyticsConsent());
        }
        if (req.photoUrl() != null) {
            c.setPhotoUrl(blank(req.photoUrl()));
        }

        if (p != null) {
            p.setMobile(newMobile);
            p.setEmail(newEmail);
            if (req.postcode() != null) {
                p.setPostcode(blank(req.postcode()));
            }
            if (req.dateOfBirth() != null) {
                p.setDateOfBirth(req.dateOfBirth());
            }
            if (req.preferredLocationId() != null) {
                p.setPreferredLocationId(blank(req.preferredLocationId()));
            }
        }

        Customer saved = customerRepository.save(c);
        CustomerResponse after = mapper.toResponse(saved);
        auditService.record(AUDIT_CUSTOMER, saved.getPublicId(), AuditAction.UPDATE, before, after);
        outbox.publish(AUDIT_CUSTOMER, saved.getPublicId(), EVENT_CUSTOMER_UPDATED, after);
        return after;
    }

    /** FR-204 — hard delete only when zero history (consent log / segment membership); otherwise reject. */
    @Transactional
    public void delete(String publicId) {
        Customer c = load(publicId);
        if (historyGuard.hasHistory(c.getId())) {
            throw new DomainException(ErrorCode.REFERENCED,
                "Customer '" + publicId + "' has history and cannot be deleted; anonymize instead");
        }
        CustomerResponse before = mapper.toResponse(c);
        c.softDelete();
        customerRepository.save(c);
        auditService.record(AUDIT_CUSTOMER, publicId, AuditAction.DELETE, before, null);
    }

    // --- internals -----------------------------------------------------------

    private void appendConsentLog(Customer c, ConsentChannel channel, boolean granted, Instant now, String actor) {
        ConsentLog log = new ConsentLog();
        log.setCustomer(c);
        log.setChannel(channel);
        log.setGranted(granted);
        log.setRecordedAt(now);
        log.setRecordedBy(actor);
        consentLogRepository.save(log);
    }

    private void assertContactUnique(String mobile, String email, Long excludeId) {
        if (mobile == null && email == null) {
            return;
        }
        List<Long> hits = excludeId == null
            ? customerRepository.findIdsByMobileOrEmail(mobile, email)
            : customerRepository.findIdsByMobileOrEmailExcluding(mobile, email, excludeId);
        if (!hits.isEmpty()) {
            throw new DomainException(ErrorCode.DUPLICATE, "A customer with this mobile or email already exists");
        }
    }

    private static String blank(String v) {
        if (v == null) {
            return null;
        }
        String trimmed = v.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeEmail(String v) {
        String b = blank(v);
        return b == null ? null : b.toLowerCase();
    }

    Customer load(String publicId) {
        return customerRepository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("Customer", publicId));
    }

    private void checkVersion(Customer c, Long requestVersion) {
        if (requestVersion != null && requestVersion != c.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK, ErrorCode.OPTIMISTIC_LOCK.defaultDetail());
        }
    }
}

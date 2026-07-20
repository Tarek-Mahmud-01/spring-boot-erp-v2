package com.springboot.erp.modules.crm.customers.service;

import com.springboot.erp.modules.crm.customers.domain.ConsentChannel;
import com.springboot.erp.modules.crm.customers.domain.ConsentLog;
import com.springboot.erp.modules.crm.customers.domain.Customer;
import com.springboot.erp.modules.crm.customers.domain.CustomerProfile;
import com.springboot.erp.modules.crm.customers.domain.CustomerStatus;
import com.springboot.erp.modules.crm.customers.dto.CustomerDtos.ConsentToggleRequest;
import com.springboot.erp.modules.crm.customers.dto.CustomerDtos.CustomerResponse;
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
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side use-cases split out of {@link CustomerCommandService} for the
 * consent-toggle (FR-201) and anonymize (FR-203) flows on ENT-050 Customer /
 * ENT-051 CustomerProfile (US-038). Every consent change appends an
 * immutable {@link ConsentLog} row in addition to the platform audit trail.
 */
@Service
public class CustomerConsentService {

    static final String AUDIT_CUSTOMER = "crm.customer";
    static final String AUDIT_CONSENT = "crm.customer_consent";
    static final String EVENT_CUSTOMER_ANONYMIZED = "crm.customer.anonymized";
    static final String EVENT_CONSENT_CHANGED = "crm.customer.consent_changed";

    private static final String ANONYMOUS_TAG = "ANONYMIZED";

    private final CustomerRepository customerRepository;
    private final ConsentLogRepository consentLogRepository;
    private final CustomersMapper mapper;
    private final AuditService auditService;
    private final OutboxPublisher outbox;
    private final CurrentUser currentUser;

    public CustomerConsentService(CustomerRepository customerRepository,
                                  ConsentLogRepository consentLogRepository,
                                  CustomersMapper mapper, AuditService auditService,
                                  OutboxPublisher outbox, CurrentUser currentUser) {
        this.customerRepository = customerRepository;
        this.consentLogRepository = consentLogRepository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.outbox = outbox;
        this.currentUser = currentUser;
    }

    /** FR-201 — toggle one consent channel; records timestamp + capturing user and an append-only log row. */
    @Transactional
    public CustomerResponse setConsent(String publicId, ConsentToggleRequest req) {
        Customer c = load(publicId);
        if (c.isAnonymized()) {
            throw new DomainException(ErrorCode.CONFLICT, "Customer '" + publicId + "' is anonymized and read-only");
        }
        CustomerProfile p = c.getProfile();
        if (p == null) {
            throw new DomainException(ErrorCode.CONFLICT, "Customer '" + publicId + "' has no profile");
        }
        ConsentChannel channel = ConsentChannel.valueOf(req.channel());
        boolean granted = req.granted();
        Instant now = Instant.now(Clock.systemUTC());
        String actor = currentUser.optional().map(pr -> pr.userPublicId()).orElse(null);

        boolean before = channel == ConsentChannel.EMAIL ? p.isConsentEmail() : p.isConsentSms();
        if (channel == ConsentChannel.EMAIL) {
            p.setConsentEmail(granted);
            p.setConsentEmailAt(now);
            p.setConsentEmailBy(actor);
        } else {
            p.setConsentSms(granted);
            p.setConsentSmsAt(now);
            p.setConsentSmsBy(actor);
        }
        customerRepository.save(c);
        appendConsentLog(c, channel, granted, now, actor);

        auditService.record(AUDIT_CONSENT, c.getPublicId(), AuditAction.UPDATE,
            Map.of("channel", channel.name(), "granted", before),
            Map.of("channel", channel.name(), "granted", granted));
        outbox.publish(AUDIT_CONSENT, c.getPublicId(), EVENT_CONSENT_CHANGED,
            Map.of("channel", channel.name(), "granted", granted));
        return mapper.toResponse(c);
    }

    /** FR-203 — replace personal fields with an anonymous tag; history rows are preserved. */
    @Transactional
    public CustomerResponse anonymize(String publicId) {
        Customer c = load(publicId);
        if (c.isAnonymized()) {
            return mapper.toResponse(c);
        }
        CustomerResponse before = mapper.toResponse(c);
        Instant now = Instant.now(Clock.systemUTC());

        c.setFirstName(ANONYMOUS_TAG);
        c.setLastName(ANONYMOUS_TAG);
        c.setPhotoUrl(null);
        c.setAnalyticsConsent(false);
        c.setStatus(CustomerStatus.ANONYMIZED);
        c.setAnonymized(true);
        c.setAnonymizedAt(now);

        CustomerProfile p = c.getProfile();
        if (p != null) {
            p.setMobile(null);
            p.setEmail(null);
            p.setPostcode(null);
            p.setDateOfBirth(null);
            p.setPreferredLocationId(null);
            p.setConsentEmail(false);
            p.setConsentSms(false);
        }

        Customer saved = customerRepository.save(c);
        CustomerResponse after = mapper.toResponse(saved);
        auditService.record(AUDIT_CUSTOMER, saved.getPublicId(), AuditAction.UPDATE, before, after);
        outbox.publish(AUDIT_CUSTOMER, saved.getPublicId(), EVENT_CUSTOMER_ANONYMIZED, after);
        return after;
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

    private Customer load(String publicId) {
        return customerRepository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("Customer", publicId));
    }
}

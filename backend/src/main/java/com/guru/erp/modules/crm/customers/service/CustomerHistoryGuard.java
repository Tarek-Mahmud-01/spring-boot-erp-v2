package com.guru.erp.modules.crm.customers.service;

import com.guru.erp.modules.crm.customers.repository.ConsentLogRepository;
import com.guru.erp.modules.crm.customers.repository.CustomerSegmentMemberRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * FR-204 hard-delete guard — a customer with any consent-log history or
 * segment membership cannot be hard-deleted (reference {@code
 * CustomerHasHistoryError}); the caller must anonymize instead. This module
 * doesn't yet own loyalty/transaction projections, so those probes aren't
 * wired here — a future slice can extend this guard in-module.
 */
@Component
public class CustomerHistoryGuard {

    private final ConsentLogRepository consentLogRepository;
    private final CustomerSegmentMemberRepository segmentMemberRepository;

    public CustomerHistoryGuard(ConsentLogRepository consentLogRepository,
                                CustomerSegmentMemberRepository segmentMemberRepository) {
        this.consentLogRepository = consentLogRepository;
        this.segmentMemberRepository = segmentMemberRepository;
    }

    public boolean hasHistory(Long customerId) {
        if (!consentLogRepository.findByCustomerIdOrderByRecordedAtDesc(customerId, PageRequest.of(0, 1)).isEmpty()) {
            return true;
        }
        return segmentMemberRepository.existsByCustomerId(customerId);
    }
}

package com.springboot.erp.modules.crm.customers.service;

import com.springboot.erp.modules.crm.customers.domain.Customer;
import com.springboot.erp.modules.crm.customers.domain.CustomerStatus;
import com.springboot.erp.modules.crm.customers.dto.CustomerDtos.ConsentLogEntry;
import com.springboot.erp.modules.crm.customers.dto.CustomerDtos.CustomerResponse;
import com.springboot.erp.modules.crm.customers.mapper.CustomersMapper;
import com.springboot.erp.modules.crm.customers.repository.ConsentLogRepository;
import com.springboot.erp.modules.crm.customers.repository.CustomerRepository;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.web.PageResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-side use-cases for ENT-050 Customer (US-038 list/get, US-040 lookup,
 * consent-log history). FR-210/211/214 — lookup matches by exact mobile /
 * email / membership id, capped at {@link #MAX_LOOKUP_RESULTS}, and never
 * returns an anonymized customer.
 */
@Service
public class CustomerQueryService {

    static final int MAX_LOOKUP_RESULTS = 10;

    private final CustomerRepository repository;
    private final ConsentLogRepository consentLogRepository;
    private final CustomersMapper mapper;

    public CustomerQueryService(CustomerRepository repository, ConsentLogRepository consentLogRepository,
                                CustomersMapper mapper) {
        this.repository = repository;
        this.consentLogRepository = consentLogRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> list(String companyId, String status, String query, Pageable pageable) {
        String q = blankToNull(query);
        CustomerStatus s = status == null || status.isBlank() ? null : CustomerStatus.valueOf(status);
        String likeTerm = q == null ? null : "%" + q.toLowerCase() + "%";
        return PageResponse.of(repository.search(blankToNull(companyId), s, likeTerm, pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(String publicId) {
        return mapper.toResponse(load(publicId));
    }

    /** FR-210/211/214 — search by exact mobile / email / membership id; cap at 10 results. */
    @Transactional(readOnly = true)
    public List<CustomerResponse> lookup(String companyId, String query) {
        String term = query.strip().toLowerCase();
        List<Customer> hits = repository.lookupExact(blankToNull(companyId), term);
        return hits.stream().limit(MAX_LOOKUP_RESULTS).map(mapper::toResponse).toList();
    }

    /** FR-201 — the customer's append-only consent-change history, newest first. */
    @Transactional(readOnly = true)
    public PageResponse<ConsentLogEntry> consentHistory(String publicId, Pageable pageable) {
        Customer c = load(publicId);
        return PageResponse.of(
            consentLogRepository.findByCustomerIdOrderByRecordedAtDesc(c.getId(), pageable), mapper::toResponse);
    }

    private Customer load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("Customer", publicId));
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}

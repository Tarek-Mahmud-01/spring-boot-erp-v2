package com.springboot.erp.modules.crm.customers.service;

import com.springboot.erp.modules.crm.customers.domain.CustomerSegment;
import com.springboot.erp.modules.crm.customers.domain.CustomerSegmentMember;
import com.springboot.erp.modules.crm.customers.domain.SegmentMode;
import com.springboot.erp.modules.crm.customers.dto.SegmentDtos.SegmentMemberChangeRequest;
import com.springboot.erp.modules.crm.customers.dto.SegmentDtos.SegmentResponse;
import com.springboot.erp.modules.crm.customers.mapper.CustomersMapper;
import com.springboot.erp.modules.crm.customers.repository.CustomerRepository;
import com.springboot.erp.modules.crm.customers.repository.CustomerSegmentMemberRepository;
import com.springboot.erp.modules.crm.customers.repository.CustomerSegmentRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.outbox.OutboxPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side use-cases split out of {@link SegmentCommandService} for
 * incremental membership changes on a STATIC {@link CustomerSegment}
 * (US-042 / FR-219-220) — add/remove a batch of customers without
 * replacing the whole member list (that full-replace path lives in
 * {@code SegmentCommandService.update}).
 */
@Service
public class SegmentMemberService {

    static final String AUDIT_SEGMENT = "crm.customer_segment";
    static final String EVENT_SEGMENT_MEMBERS_CHANGED = "crm.segment.members_changed";

    private final CustomerSegmentRepository segmentRepository;
    private final CustomerSegmentMemberRepository memberRepository;
    private final CustomerRepository customerRepository;
    private final CustomersMapper mapper;
    private final AuditService auditService;
    private final OutboxPublisher outbox;

    public SegmentMemberService(CustomerSegmentRepository segmentRepository,
                                CustomerSegmentMemberRepository memberRepository,
                                CustomerRepository customerRepository, CustomersMapper mapper,
                                AuditService auditService, OutboxPublisher outbox) {
        this.segmentRepository = segmentRepository;
        this.memberRepository = memberRepository;
        this.customerRepository = customerRepository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.outbox = outbox;
    }

    /** POST .../members — add a batch of customers to a STATIC segment. */
    @Transactional
    public SegmentResponse addMembers(String publicId, SegmentMemberChangeRequest req) {
        CustomerSegment s = requireStatic(load(publicId));
        Instant now = Instant.now(Clock.systemUTC());
        for (Long customerId : resolveCustomerIds(s.getCompanyId(), req.customerIds())) {
            if (!memberRepository.existsBySegmentIdAndCustomerId(s.getId(), customerId)) {
                CustomerSegmentMember m = new CustomerSegmentMember();
                m.setSegment(s);
                m.setCustomer(customerRepository.getReferenceById(customerId));
                m.setAddedAt(now);
                memberRepository.save(m);
            }
        }
        return afterMemberChange(s);
    }

    /** DELETE .../members — remove a batch of customers from a STATIC segment. */
    @Transactional
    public SegmentResponse removeMembers(String publicId, SegmentMemberChangeRequest req) {
        CustomerSegment s = requireStatic(load(publicId));
        for (Long customerId : resolveCustomerIds(s.getCompanyId(), req.customerIds())) {
            memberRepository.findBySegmentIdAndCustomerId(s.getId(), customerId).ifPresent(memberRepository::delete);
        }
        return afterMemberChange(s);
    }

    // --- internals -----------------------------------------------------------

    private SegmentResponse afterMemberChange(CustomerSegment s) {
        List<String> memberIds = memberPublicIds(s);
        SegmentResponse after = mapper.toResponse(s, memberIds);
        auditService.record(AUDIT_SEGMENT, s.getPublicId(), AuditAction.UPDATE, null,
            Map.of("id", s.getPublicId(), "memberCount", memberIds.size()));
        outbox.publish(AUDIT_SEGMENT, s.getPublicId(), EVENT_SEGMENT_MEMBERS_CHANGED, after);
        return after;
    }

    private CustomerSegment requireStatic(CustomerSegment s) {
        if (s.getMode() != SegmentMode.STATIC) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Segment '" + s.getPublicId() + "' is DYNAMIC; membership is derived from its rules, not edited directly");
        }
        return s;
    }

    private List<Long> resolveCustomerIds(String companyId, List<String> publicIds) {
        if (publicIds == null || publicIds.isEmpty()) {
            return List.of();
        }
        return customerRepository.findByCompanyIdAndPublicIdIn(companyId, publicIds)
            .stream().map(c -> c.getId()).toList();
    }

    private List<String> memberPublicIds(CustomerSegment s) {
        return memberRepository.findBySegmentId(s.getId()).stream()
            .map(m -> m.getCustomer().getPublicId())
            .sorted()
            .toList();
    }

    private CustomerSegment load(String publicId) {
        return segmentRepository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("CustomerSegment", publicId));
    }
}

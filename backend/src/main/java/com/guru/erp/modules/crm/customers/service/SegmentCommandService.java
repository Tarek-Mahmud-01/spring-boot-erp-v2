package com.guru.erp.modules.crm.customers.service;

import com.guru.erp.modules.crm.customers.domain.Customer;
import com.guru.erp.modules.crm.customers.domain.CustomerSegment;
import com.guru.erp.modules.crm.customers.domain.CustomerSegmentMember;
import com.guru.erp.modules.crm.customers.domain.SegmentMode;
import com.guru.erp.modules.crm.customers.dto.SegmentDtos.SegmentCreateRequest;
import com.guru.erp.modules.crm.customers.dto.SegmentDtos.SegmentResponse;
import com.guru.erp.modules.crm.customers.dto.SegmentDtos.SegmentRule;
import com.guru.erp.modules.crm.customers.dto.SegmentDtos.SegmentUpdateRequest;
import com.guru.erp.modules.crm.customers.mapper.CustomersMapper;
import com.guru.erp.modules.crm.customers.repository.CustomerRepository;
import com.guru.erp.modules.crm.customers.repository.CustomerSegmentMemberRepository;
import com.guru.erp.modules.crm.customers.repository.CustomerSegmentRepository;
import com.guru.erp.platform.audit.AuditAction;
import com.guru.erp.platform.audit.AuditService;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import com.guru.erp.platform.outbox.OutboxPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side use-cases for ENT-073 CustomerSegment (US-042 / FR-219-222):
 * create / update / delete. Ports the reference flows: unique
 * {@code (company, code)}, a STATIC segment's member list is fully
 * replaceable at creation or via update, and a segment with linked
 * promotions cannot be deleted (reference
 * {@code SegmentHasLinkedPromotionsError}). Incremental member add/remove
 * lives in {@link SegmentMemberService}.
 */
@Service
public class SegmentCommandService {

    static final String AUDIT_SEGMENT = "crm.customer_segment";
    static final String EVENT_SEGMENT_CREATED = "crm.segment.created";
    static final String EVENT_SEGMENT_UPDATED = "crm.segment.updated";

    private final CustomerSegmentRepository segmentRepository;
    private final CustomerSegmentMemberRepository memberRepository;
    private final CustomerRepository customerRepository;
    private final CustomersMapper mapper;
    private final AuditService auditService;
    private final OutboxPublisher outbox;

    public SegmentCommandService(CustomerSegmentRepository segmentRepository,
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

    /** FR-219/220 — create a segment; STATIC segments may seed their member list at creation. */
    @Transactional
    public SegmentResponse create(SegmentCreateRequest req) {
        String code = req.code().strip();
        if (segmentRepository.existsByCompanyIdAndCode(req.companyId(), code)) {
            throw new DomainException(ErrorCode.DUPLICATE,
                "A segment with code '" + code + "' already exists for this company");
        }
        CustomerSegment s = new CustomerSegment();
        s.setCompanyId(req.companyId());
        s.setCode(code);
        s.setName(req.name().strip());
        s.setDescription(req.description());
        s.setMode(req.mode() == null ? SegmentMode.STATIC : SegmentMode.valueOf(req.mode()));
        s.setDefinition(toDefinition(req.rules()));
        s.setRuleLogic(req.ruleLogic() == null ? "AND" : req.ruleLogic());

        CustomerSegment saved = segmentRepository.save(s);

        if (saved.getMode() == SegmentMode.STATIC && req.memberIds() != null && !req.memberIds().isEmpty()) {
            replaceMembers(saved, resolveCustomerIds(req.companyId(), req.memberIds()));
        }

        List<String> memberIds = memberPublicIds(saved);
        SegmentResponse after = mapper.toResponse(saved, memberIds);
        auditService.record(AUDIT_SEGMENT, saved.getPublicId(), AuditAction.CREATE, null, after);
        outbox.publish(AUDIT_SEGMENT, saved.getPublicId(), EVENT_SEGMENT_CREATED, after);
        return after;
    }

    /** FR-219/221 — partial update; a static segment's member list is fully replaced when supplied. */
    @Transactional
    public SegmentResponse update(String publicId, SegmentUpdateRequest req) {
        CustomerSegment s = load(publicId);
        checkVersion(s, req.version());
        SegmentResponse before = mapper.toResponse(s, memberPublicIds(s));

        if (req.name() != null) {
            s.setName(req.name().strip());
        }
        if (req.description() != null) {
            s.setDescription(req.description().isBlank() ? null : req.description());
        }
        if (req.rules() != null) {
            s.setDefinition(toDefinition(req.rules()));
        }
        if (req.ruleLogic() != null) {
            s.setRuleLogic(req.ruleLogic());
        }
        if (req.linkedPromotionIds() != null) {
            s.setLinkedPromotionIds(new ArrayList<>(req.linkedPromotionIds()));
        }
        if (req.memberIds() != null && s.getMode() == SegmentMode.STATIC) {
            replaceMembers(s, resolveCustomerIds(s.getCompanyId(), req.memberIds()));
        }

        CustomerSegment saved = segmentRepository.save(s);
        List<String> memberIds = memberPublicIds(saved);
        SegmentResponse after = mapper.toResponse(saved, memberIds);
        auditService.record(AUDIT_SEGMENT, saved.getPublicId(), AuditAction.UPDATE, before, after);
        outbox.publish(AUDIT_SEGMENT, saved.getPublicId(), EVENT_SEGMENT_UPDATED, after);
        return after;
    }

    /** FR-221 — a segment linked to active promotions cannot be silently removed. */
    @Transactional
    public void delete(String publicId) {
        CustomerSegment s = load(publicId);
        if (!s.getLinkedPromotionIds().isEmpty()) {
            throw new DomainException(ErrorCode.REFERENCED,
                "Segment '" + publicId + "' is linked to promotions and cannot be deleted; unlink first");
        }
        SegmentResponse before = mapper.toResponse(s, memberPublicIds(s));
        s.softDelete();
        segmentRepository.save(s);
        auditService.record(AUDIT_SEGMENT, publicId, AuditAction.DELETE, before, null);
    }

    // --- internals -----------------------------------------------------------

    private void replaceMembers(CustomerSegment s, List<Long> customerIds) {
        memberRepository.deleteBySegmentId(s.getId());
        Instant now = Instant.now(Clock.systemUTC());
        Set<Long> seen = new LinkedHashSet<>();
        for (Long id : customerIds) {
            if (!seen.add(id)) {
                continue;
            }
            CustomerSegmentMember m = new CustomerSegmentMember();
            m.setSegment(s);
            m.setCustomer(customerRepository.getReferenceById(id));
            m.setAddedAt(now);
            memberRepository.save(m);
        }
    }

    private List<Long> resolveCustomerIds(String companyId, List<String> publicIds) {
        if (publicIds == null || publicIds.isEmpty()) {
            return List.of();
        }
        return customerRepository.findByCompanyIdAndPublicIdIn(companyId, publicIds)
            .stream().map(Customer::getId).toList();
    }

    private List<String> memberPublicIds(CustomerSegment s) {
        return memberRepository.findBySegmentId(s.getId()).stream()
            .map(m -> m.getCustomer().getPublicId())
            .sorted()
            .toList();
    }

    private List<Map<String, Object>> toDefinition(List<SegmentRule> rules) {
        if (rules == null) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (SegmentRule r : rules) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("field", r.field() == null ? null : r.field().toUpperCase());
            row.put("op", r.op() == null ? null : r.op().toUpperCase());
            row.put("value", r.value());
            if (r.windowDays() != null) {
                row.put("window_days", r.windowDays());
            }
            if (r.currency() != null) {
                row.put("currency", r.currency());
            }
            out.add(row);
        }
        return out;
    }

    CustomerSegment load(String publicId) {
        return segmentRepository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("CustomerSegment", publicId));
    }

    private void checkVersion(CustomerSegment s, Long requestVersion) {
        if (requestVersion != null && requestVersion != s.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK, ErrorCode.OPTIMISTIC_LOCK.defaultDetail());
        }
    }
}

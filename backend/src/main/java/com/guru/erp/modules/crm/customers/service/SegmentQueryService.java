package com.guru.erp.modules.crm.customers.service;

import com.guru.erp.modules.crm.customers.domain.CustomerSegment;
import com.guru.erp.modules.crm.customers.dto.SegmentDtos.SegmentResponse;
import com.guru.erp.modules.crm.customers.mapper.CustomersMapper;
import com.guru.erp.modules.crm.customers.repository.CustomerSegmentMemberRepository;
import com.guru.erp.modules.crm.customers.repository.CustomerSegmentRepository;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.web.PageResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-side use-cases for ENT-073 CustomerSegment: list (optionally by company) and get. */
@Service
public class SegmentQueryService {

    private final CustomerSegmentRepository repository;
    private final CustomerSegmentMemberRepository memberRepository;
    private final CustomersMapper mapper;

    public SegmentQueryService(CustomerSegmentRepository repository,
                               CustomerSegmentMemberRepository memberRepository, CustomersMapper mapper) {
        this.repository = repository;
        this.memberRepository = memberRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<SegmentResponse> list(String companyId, Pageable pageable) {
        String c = companyId == null || companyId.isBlank() ? null : companyId.trim();
        return PageResponse.of(repository.search(c, pageable), s -> mapper.toResponse(s, memberIds(s)));
    }

    @Transactional(readOnly = true)
    public SegmentResponse get(String publicId) {
        CustomerSegment s = load(publicId);
        return mapper.toResponse(s, memberIds(s));
    }

    private List<String> memberIds(CustomerSegment s) {
        return memberRepository.findBySegmentId(s.getId()).stream()
            .map(m -> m.getCustomer().getPublicId())
            .sorted()
            .toList();
    }

    private CustomerSegment load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("CustomerSegment", publicId));
    }
}

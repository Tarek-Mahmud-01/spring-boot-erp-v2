package com.guru.erp.modules.crm.customers.repository;

import com.guru.erp.modules.crm.customers.domain.CustomerSegmentMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link CustomerSegmentMember} membership rows. */
public interface CustomerSegmentMemberRepository extends JpaRepository<CustomerSegmentMember, Long> {

    Page<CustomerSegmentMember> findBySegmentIdOrderByIdAsc(Long segmentId, Pageable pageable);

    List<CustomerSegmentMember> findBySegmentId(Long segmentId);

    Optional<CustomerSegmentMember> findBySegmentIdAndCustomerId(Long segmentId, Long customerId);

    void deleteBySegmentId(Long segmentId);

    boolean existsBySegmentIdAndCustomerId(Long segmentId, Long customerId);

    boolean existsByCustomerId(Long customerId);
}

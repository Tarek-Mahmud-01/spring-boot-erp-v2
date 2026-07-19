package com.guru.erp.modules.crm.customers.repository;

import com.guru.erp.modules.crm.customers.domain.Customer;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link Customer}. Soft-deleted rows are excluded
 * automatically by the {@code @SQLRestriction} on {@code BaseEntity}.
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPublicId(String publicId);

    boolean existsByMembershipId(String membershipId);

    /** FR-200 — mobile / email each unique per tenant when present. */
    @Query("""
        select c.id from Customer c join c.profile p
        where (:mobile is not null and p.mobile = :mobile)
           or (:email is not null and p.email = :email)
        """)
    java.util.List<Long> findIdsByMobileOrEmail(@Param("mobile") String mobile, @Param("email") String email);

    @Query("""
        select c.id from Customer c join c.profile p
        where ((:mobile is not null and p.mobile = :mobile)
            or (:email is not null and p.email = :email))
          and c.id <> :excludeId
        """)
    java.util.List<Long> findIdsByMobileOrEmailExcluding(@Param("mobile") String mobile,
                                                          @Param("email") String email,
                                                          @Param("excludeId") Long excludeId);

    /** US-038 list — free-text search over name / membership id / mobile / email, filtered by company/status. */
    @Query("""
        select c from Customer c left join c.profile p
        where (:companyId is null or c.companyId = :companyId)
          and (:status is null or c.status = :status)
          and (:query is null
               or lower(c.firstName) like :query
               or lower(c.lastName) like :query
               or lower(c.membershipId) like :query
               or lower(p.mobile) like :query
               or lower(p.email) like :query)
        order by c.createdAt desc
        """)
    Page<Customer> search(@Param("companyId") String companyId,
                          @Param("status") com.guru.erp.modules.crm.customers.domain.CustomerStatus status,
                          @Param("query") String query,
                          Pageable pageable);

    /** FR-210/211 — lookup by exact mobile/email/membership id match, capped by the pageable size. */
    @Query("""
        select c from Customer c left join c.profile p
        where c.anonymized = false
          and (:companyId is null or c.companyId = :companyId)
          and (lower(p.mobile) = :term or lower(p.email) = :term or lower(c.membershipId) = :term)
        """)
    java.util.List<Customer> lookupExact(@Param("companyId") String companyId, @Param("term") String term);

    java.util.List<Customer> findByCompanyIdAndPublicIdIn(String companyId, java.util.List<String> publicIds);
}

package com.guru.erp.modules.procurement.bills.repository;

import com.guru.erp.modules.procurement.bills.domain.AmountReceived;
import com.guru.erp.modules.procurement.bills.domain.AmountReceivedStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link AmountReceived}. Soft-deleted rows are auto-excluded by BaseEntity. */
public interface AmountReceivedRepository extends JpaRepository<AmountReceived, Long> {

    Optional<AmountReceived> findByPublicId(String publicId);

    boolean existsByNumber(String number);

    /** Highest number for the RCP-YYYY-NNNN scheme in the given year prefix. */
    @Query("""
        select max(r.number) from AmountReceived r
        where r.number like concat(:prefix, '%')
        """)
    String maxNumberForPrefix(@Param("prefix") String prefix);

    @Query("""
        select r from AmountReceived r
        where (:supplierId is null or r.supplierId = :supplierId)
          and (:status is null or r.status = :status)
          and (:search is null
               or lower(r.number) like lower(concat('%', :search, '%'))
               or lower(r.creditNoteReference) like lower(concat('%', :search, '%'))
               or lower(r.referenceNo) like lower(concat('%', :search, '%')))
        order by r.createdAt desc
        """)
    Page<AmountReceived> search(@Param("supplierId") String supplierId,
                                @Param("status") AmountReceivedStatus status,
                                @Param("search") String search,
                                Pageable pageable);
}

package com.guru.erp.modules.finance.gl.repository;

import com.guru.erp.modules.finance.gl.domain.JournalEntry;
import com.guru.erp.modules.finance.gl.domain.JournalEntryStatus;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link JournalEntry}. Soft-deleted rows excluded by {@code BaseEntity}'s {@code @SQLRestriction}. */
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    Optional<JournalEntry> findByPublicId(String publicId);

    boolean existsByCompanyIdAndVoucherNumber(String companyId, String voucherNumber);

    /** Used by {@code VoucherTypeService.delete} to block deleting a code still referenced by a journal entry. */
    boolean existsByVoucherType(String voucherType);

    long countByCompanyIdAndVoucherTypeAndEntryDateBetween(
        String companyId, String voucherType, LocalDate from, LocalDate to);

    /** List with optional company/status/voucherType/date-range/free-text filters. */
    @Query("""
        select e from JournalEntry e
        where e.companyId = :companyId
          and (:status is null or e.status = :status)
          and (:voucherType is null or e.voucherType = :voucherType)
          and (:fromDate is null or e.entryDate >= :fromDate)
          and (:toDate is null or e.entryDate <= :toDate)
          and (:search is null
               or lower(e.voucherNumber) like lower(concat('%', :search, '%'))
               or lower(e.reference) like lower(concat('%', :search, '%'))
               or lower(e.narration) like lower(concat('%', :search, '%')))
        order by e.entryDate desc, e.id desc
        """)
    Page<JournalEntry> search(@Param("companyId") String companyId,
                              @Param("status") JournalEntryStatus status,
                              @Param("voucherType") String voucherType,
                              @Param("fromDate") LocalDate fromDate,
                              @Param("toDate") LocalDate toDate,
                              @Param("search") String search,
                              Pageable pageable);
}

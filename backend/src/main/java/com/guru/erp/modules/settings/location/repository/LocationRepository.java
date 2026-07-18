package com.guru.erp.modules.settings.location.repository;

import com.guru.erp.modules.settings.location.domain.Location;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence for {@link Location}. Soft-deleted rows are hidden automatically by
 * {@code @SQLRestriction("deleted_at is null")} on the base entity, so no finder
 * needs to filter {@code deleted_at} explicitly. The parent {@code company} is
 * fetched via an entity graph on reads so mapping to a DTO never triggers an N+1.
 */
public interface LocationRepository extends JpaRepository<Location, Long> {

    @EntityGraph(attributePaths = {"company"})
    Optional<Location> findByPublicId(String publicId);

    /** AC-002-1: code is unique within a company (keyed on the company ULID). */
    boolean existsByCompany_PublicIdAndCode(String companyPublicId, String code);

    /** Uniqueness check for update — excludes the row being edited. */
    boolean existsByCompany_PublicIdAndCodeAndIdNot(String companyPublicId, String code, Long id);

    /**
     * Paged list with optional filters, oldest first (matching the reference
     * {@code order_by(created_at)}). A null filter argument disables that clause.
     * {@code q} does a case-insensitive contains match on name or code.
     */
    @EntityGraph(attributePaths = {"company"})
    @Query("""
        select l from Location l
        where (:companyPublicId is null or l.company.publicId = :companyPublicId)
          and (:status is null or l.status = :status)
          and (:type is null or l.type = :type)
          and (:q is null
               or lower(l.name) like lower(concat('%', :q, '%'))
               or lower(l.code) like lower(concat('%', :q, '%')))
        order by l.createdAt asc
        """)
    Page<Location> search(
        @Param("companyPublicId") String companyPublicId,
        @Param("status") String status,
        @Param("type") String type,
        @Param("q") String q,
        Pageable pageable);
}

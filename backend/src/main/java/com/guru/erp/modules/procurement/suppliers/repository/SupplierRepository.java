package com.guru.erp.modules.procurement.suppliers.repository;

import com.guru.erp.modules.procurement.suppliers.domain.Supplier;
import com.guru.erp.modules.procurement.suppliers.domain.SupplierStatus;
import com.guru.erp.modules.procurement.suppliers.domain.SupplierType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link Supplier}. Soft-deleted rows are excluded automatically by the
 * {@code @SQLRestriction} on {@code BaseEntity}, matching the reference {@code deleted_at IS NULL}
 * list filter.
 */
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findByPublicId(String publicId);

    boolean existsByCode(String code);

    long count();

    /**
     * List with optional status, type, location, and free-text (name OR code) filters — mirrors the
     * reference {@code list_suppliers}. Ordered by code, server-paged.
     */
    @Query("""
        select s from Supplier s
        where (:status is null or s.status = :status)
          and (:type is null or s.type = :type)
          and (:locationId is null or s.locationId = :locationId)
          and (:search is null
               or lower(s.name) like lower(concat('%', :search, '%'))
               or lower(s.code) like lower(concat('%', :search, '%')))
        order by s.code asc
        """)
    Page<Supplier> search(@Param("status") SupplierStatus status,
                          @Param("type") SupplierType type,
                          @Param("locationId") String locationId,
                          @Param("search") String search,
                          Pageable pageable);
}

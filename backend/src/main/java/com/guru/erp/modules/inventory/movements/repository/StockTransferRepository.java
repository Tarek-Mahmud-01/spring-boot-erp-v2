package com.guru.erp.modules.inventory.movements.repository;

import com.guru.erp.modules.inventory.movements.domain.StockTransfer;
import com.guru.erp.modules.inventory.movements.domain.TransferStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link StockTransfer}. The {@code @SQLRestriction} on {@code BaseEntity} excludes
 * soft-deleted rows from every query automatically.
 */
public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {

    Optional<StockTransfer> findByPublicId(String publicId);

    boolean existsByNumber(String number);

    /** List with optional location (either side), status, and free-text number filters. */
    @Query("""
        select t from StockTransfer t
        where (:locationId is null
               or t.sourceLocationId = :locationId
               or t.destinationLocationId = :locationId)
          and (:status is null or t.status = :status)
          and (:search is null or lower(t.number) like lower(concat('%', :search, '%')))
        order by t.createdAt desc
        """)
    Page<StockTransfer> search(@Param("locationId") String locationId,
                               @Param("status") TransferStatus status,
                               @Param("search") String search,
                               Pageable pageable);
}

package com.guru.erp.modules.inventory.counts.repository;

import com.guru.erp.modules.inventory.counts.domain.CycleCountPlan;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for {@link CycleCountPlan}. Soft-deleted rows are excluded
 * automatically by the {@code @SQLRestriction} on {@code BaseEntity}. The
 * {@code lines} graph is eager-loaded so response mapping avoids N+1.
 */
public interface CycleCountPlanRepository extends JpaRepository<CycleCountPlan, Long> {

    @EntityGraph(attributePaths = "lines")
    Optional<CycleCountPlan> findByPublicId(String publicId);

    boolean existsByNumber(String number);

    long countByNumberStartingWith(String prefix);

    @EntityGraph(attributePaths = "lines")
    Page<CycleCountPlan> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "lines")
    Page<CycleCountPlan> findByLocationIdOrderByCreatedAtDesc(String locationId, Pageable pageable);
}

package com.springboot.erp.modules.product.promotions.repository;

import com.springboot.erp.modules.product.promotions.domain.LifecycleTransition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for the append-only {@link LifecycleTransition} ledger (FR-074).
 * Rows are only inserted and read, never updated or deleted by the application.
 */
public interface LifecycleTransitionRepository extends JpaRepository<LifecycleTransition, Long> {

    /**
     * The ledger for one product, newest first (mirrors the reference
     * {@code list_lifecycle_transitions} ordering by changed_at desc, id desc).
     */
    Page<LifecycleTransition> findByProductIdOrderByChangedAtDescIdDesc(String productId, Pageable pageable);

    /** Most recent transition for a product, or empty when it has never moved. */
    java.util.Optional<LifecycleTransition> findFirstByProductIdOrderByChangedAtDescIdDesc(String productId);
}

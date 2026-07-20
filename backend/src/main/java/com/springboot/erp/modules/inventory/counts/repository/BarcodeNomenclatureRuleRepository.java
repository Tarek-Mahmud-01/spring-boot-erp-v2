package com.springboot.erp.modules.inventory.counts.repository;

import com.springboot.erp.modules.inventory.counts.domain.BarcodeNomenclatureRule;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for {@link BarcodeNomenclatureRule}. Soft-deleted rows are
 * excluded automatically by the {@code @SQLRestriction} on {@code BaseEntity}.
 */
public interface BarcodeNomenclatureRuleRepository
        extends JpaRepository<BarcodeNomenclatureRule, Long> {

    Optional<BarcodeNomenclatureRule> findByPublicId(String publicId);

    Page<BarcodeNomenclatureRule> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<BarcodeNomenclatureRule> findByCompanyIdOrderByCreatedAtDesc(String companyId, Pageable pageable);
}

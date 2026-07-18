package com.guru.erp.modules.settings.company.repository;

import com.guru.erp.modules.settings.company.domain.Company;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for {@link Company}. The {@code @SQLRestriction} on
 * {@link com.guru.erp.platform.entity.BaseEntity} means every finder here
 * automatically excludes soft-deleted rows.
 */
public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByPublicId(String publicId);

    Optional<Company> findByCode(String code);

    boolean existsByCode(String code);

    /** FR-AU-001: ABN is unique across live companies. */
    boolean existsByAbn(String abn);

    /** FR-004: count live primary companies to enforce the single-primary invariant. */
    long countByPrimaryTrue();
}

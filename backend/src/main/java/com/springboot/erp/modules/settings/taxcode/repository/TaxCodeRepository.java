package com.springboot.erp.modules.settings.taxcode.repository;

import com.springboot.erp.modules.settings.taxcode.domain.TaxCode;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for {@link TaxCode}. The {@code @SQLRestriction} on
 * {@link com.springboot.erp.platform.entity.BaseEntity} already hides soft-deleted
 * rows from every query, so finders here never need a {@code deleted_at is null}
 * clause.
 */
public interface TaxCodeRepository extends JpaRepository<TaxCode, Long> {

    Optional<TaxCode> findByPublicId(String publicId);

    /**
     * All live ranges for a {@code (company, code)} pair — used by the service's
     * pure-Java overlap check (the reference's Postgres GIST exclude mirror).
     */
    List<TaxCode> findByCompanyPublicIdAndCode(String companyPublicId, String code);
}

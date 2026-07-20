package com.springboot.erp.modules.settings.numbering.repository;

import com.springboot.erp.modules.settings.numbering.domain.DocumentType;
import com.springboot.erp.modules.settings.numbering.domain.NumberingRule;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for {@link NumberingRule}. Soft-deleted rows are hidden globally by
 * the {@code @SQLRestriction} on {@code BaseEntity}, so these finders only ever
 * see live rules.
 */
public interface NumberingRuleRepository extends JpaRepository<NumberingRule, Long> {

    Optional<NumberingRule> findByPublicId(String publicId);

    /** Uniqueness guard for FR-022 — one rule per (company, document type). */
    boolean existsByCompanyIdAndDocumentType(String companyId, DocumentType documentType);

    Page<NumberingRule> findByCompanyId(String companyId, Pageable pageable);

    Page<NumberingRule> findByDocumentType(DocumentType documentType, Pageable pageable);

    Page<NumberingRule> findByCompanyIdAndDocumentType(
        String companyId, DocumentType documentType, Pageable pageable);
}

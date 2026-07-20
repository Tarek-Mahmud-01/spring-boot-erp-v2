package com.springboot.erp.modules.finance.coa.repository;

import com.springboot.erp.modules.finance.coa.domain.AccountMapping;
import com.springboot.erp.modules.finance.coa.domain.AccountModule;
import com.springboot.erp.modules.finance.coa.domain.AccountPurpose;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for {@link AccountMapping} (reference {@code views/coa_management.py}).
 * One row per (company, module, purpose) — enforced by
 * {@code uq_account_mappings_company_module_purpose}.
 */
public interface AccountMappingRepository extends JpaRepository<AccountMapping, Long> {

    Optional<AccountMapping> findByPublicId(String publicId);

    Optional<AccountMapping> findByCompanyIdAndModuleAndPurpose(String companyId, AccountModule module, AccountPurpose purpose);

    List<AccountMapping> findByCompanyIdOrderByModuleAscPurposeAsc(String companyId);

    List<AccountMapping> findByCompanyIdAndModule(String companyId, AccountModule module);
}

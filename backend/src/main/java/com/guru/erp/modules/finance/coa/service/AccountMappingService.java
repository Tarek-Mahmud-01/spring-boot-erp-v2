package com.guru.erp.modules.finance.coa.service;

import com.guru.erp.modules.finance.coa.domain.Account;
import com.guru.erp.modules.finance.coa.domain.AccountMapping;
import com.guru.erp.modules.finance.coa.domain.AccountModule;
import com.guru.erp.modules.finance.coa.domain.AccountPurpose;
import com.guru.erp.modules.finance.coa.dto.AccountMappingDtos.AccountMappingDeleteResponse;
import com.guru.erp.modules.finance.coa.dto.AccountMappingDtos.AccountMappingMutationResponse;
import com.guru.erp.modules.finance.coa.dto.AccountMappingDtos.AccountMappingResponse;
import com.guru.erp.modules.finance.coa.dto.AccountMappingDtos.AccountMappingUpsertRequest;
import com.guru.erp.modules.finance.coa.dto.AccountMappingDtos.CoaMappingStatusResponse;
import com.guru.erp.modules.finance.coa.mapper.CoaMapper;
import com.guru.erp.modules.finance.coa.repository.AccountMappingRepository;
import com.guru.erp.modules.finance.coa.repository.AccountRepository;
import com.guru.erp.platform.audit.AuditAction;
import com.guru.erp.platform.audit.AuditService;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account-mapping CRUD + per-module readiness (reference
 * {@code views/coa_management.py} AccountMappingView + {@code account_mapping_service.py}).
 * Lets ERP modules (Inventory, Procurement, Sales, POS) resolve a GL account
 * dynamically for a (module, purpose) slot instead of hard-coding a code.
 */
@Service
public class AccountMappingService {

    private static final String AUDIT_ENTITY = "account_mapping";

    /** Minimum required mappings per module — reference {@code _REQUIRED_MAPPINGS}. */
    private static final Map<AccountModule, List<AccountPurpose>> REQUIRED_MAPPINGS = buildRequiredMappings();

    private final AccountMappingRepository repository;
    private final AccountRepository accountRepository;
    private final CoaMapper mapper;
    private final AuditService auditService;

    public AccountMappingService(AccountMappingRepository repository, AccountRepository accountRepository,
                                 CoaMapper mapper, AuditService auditService) {
        this.repository = repository;
        this.accountRepository = accountRepository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AccountMappingResponse> list(String companyId) {
        return repository.findByCompanyIdOrderByModuleAscPurposeAsc(companyId).stream()
            .map(mapper::toResponse)
            .toList();
    }

    @Transactional
    public AccountMappingMutationResponse upsert(AccountMappingUpsertRequest req) {
        Account account = accountRepository.findByPublicId(req.accountId())
            .orElseThrow(() -> DomainException.notFound("Account", req.accountId()));
        if (!account.getCompanyId().equals(req.companyId())) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Account must belong to the same company as the mapping.");
        }

        var existing = repository.findByCompanyIdAndModuleAndPurpose(req.companyId(), req.module(), req.purpose());
        AccountMapping mapping;
        AuditAction action;
        Object before = null;
        if (existing.isPresent()) {
            mapping = existing.get();
            before = mapper.toResponse(mapping);
            mapping.setAccount(account);
            action = AuditAction.UPDATE;
        } else {
            mapping = new AccountMapping();
            mapping.setCompanyId(req.companyId());
            mapping.setModule(req.module());
            mapping.setPurpose(req.purpose());
            mapping.setAccount(account);
            action = AuditAction.CREATE;
        }

        AccountMapping saved = repository.save(mapping);
        AccountMappingResponse response = mapper.toResponse(saved);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), action, before, response);
        return new AccountMappingMutationResponse(response, computeStatus(req.companyId()));
    }

    @Transactional
    public AccountMappingDeleteResponse delete(String companyId, AccountModule module, AccountPurpose purpose) {
        AccountMapping mapping = repository.findByCompanyIdAndModuleAndPurpose(companyId, module, purpose)
            .orElseThrow(() -> DomainException.notFound("AccountMapping", module + "/" + purpose));
        AccountMappingResponse before = mapper.toResponse(mapping);
        String publicId = mapping.getPublicId();
        repository.delete(mapping);
        auditService.record(AUDIT_ENTITY, publicId, AuditAction.DELETE, before, null);
        return new AccountMappingDeleteResponse(computeStatus(companyId));
    }

    @Transactional(readOnly = true)
    public List<CoaMappingStatusResponse> mappingStatus(String companyId) {
        return computeStatus(companyId);
    }

    /** Returns the required purposes for {@code module} that have no mapping yet for {@code companyId}. */
    @Transactional(readOnly = true)
    public List<AccountPurpose> missingMappings(String companyId, AccountModule module) {
        List<AccountPurpose> required = REQUIRED_MAPPINGS.getOrDefault(module, List.of());
        if (required.isEmpty()) {
            return List.of();
        }
        var configured = repository.findByCompanyIdAndModule(companyId, module).stream()
            .map(AccountMapping::getPurpose)
            .collect(Collectors.toSet());
        return required.stream().filter(p -> !configured.contains(p)).toList();
    }

    private List<CoaMappingStatusResponse> computeStatus(String companyId) {
        var configuredByModule = repository.findByCompanyIdOrderByModuleAscPurposeAsc(companyId).stream()
            .collect(Collectors.groupingBy(
                AccountMapping::getModule,
                Collectors.mapping(AccountMapping::getPurpose, Collectors.toList())));

        return Arrays.stream(AccountModule.values())
            .map(module -> {
                List<AccountPurpose> required = REQUIRED_MAPPINGS.getOrDefault(module, List.of());
                List<AccountPurpose> configured = configuredByModule.getOrDefault(module, List.of());
                List<AccountPurpose> missing = missingMappings(companyId, module);
                boolean ready = missing.isEmpty() && !required.isEmpty();
                return new CoaMappingStatusResponse(module, configured, missing, ready);
            })
            .toList();
    }

    private static Map<AccountModule, List<AccountPurpose>> buildRequiredMappings() {
        Map<AccountModule, List<AccountPurpose>> map = new EnumMap<>(AccountModule.class);
        map.put(AccountModule.INVENTORY, List.of(
            AccountPurpose.INVENTORY_ASSET, AccountPurpose.INVENTORY_VARIANCE));
        map.put(AccountModule.PROCUREMENT, List.of(
            AccountPurpose.ACCOUNTS_PAYABLE, AccountPurpose.PURCHASE_ACCOUNT, AccountPurpose.GRN_CLEARING));
        map.put(AccountModule.SALES, List.of(
            AccountPurpose.ACCOUNTS_RECEIVABLE, AccountPurpose.SALES_REVENUE));
        map.put(AccountModule.FINANCE, List.of(
            AccountPurpose.EXCHANGE_GAIN_LOSS));
        map.put(AccountModule.POS, List.of(
            AccountPurpose.CASH_ON_HAND, AccountPurpose.SALES_REVENUE, AccountPurpose.GST_PAYABLE));
        return map;
    }
}

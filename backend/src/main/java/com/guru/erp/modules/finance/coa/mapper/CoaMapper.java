package com.guru.erp.modules.finance.coa.mapper;

import com.guru.erp.modules.finance.coa.domain.Account;
import com.guru.erp.modules.finance.coa.domain.AccountMapping;
import com.guru.erp.modules.finance.coa.dto.AccountDtos.AccountResponse;
import com.guru.erp.modules.finance.coa.dto.AccountMappingDtos.AccountMappingResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct entity to DTO mappers for the finance "coa" slice (ARCHITECTURE.md
 * §2). {@code id} always maps from {@code publicId}; internal bigint ids and
 * the JPA-managed {@code parent} association are never exposed directly — the
 * parent's public id is resolved explicitly since MapStruct cannot dereference
 * a lazy proxy safely inside a plain expression outside a transaction.
 */
@Mapper(componentModel = "spring")
public interface CoaMapper {

    @Mapping(target = "id", source = "account.publicId")
    @Mapping(target = "parentId", source = "parentId")
    AccountResponse toResponse(Account account, String parentId);

    @Mapping(target = "id", source = "mapping.publicId")
    @Mapping(target = "accountId", source = "mapping.account.publicId")
    @Mapping(target = "accountCode", source = "mapping.account.code")
    @Mapping(target = "accountName", source = "mapping.account.name")
    AccountMappingResponse toResponse(AccountMapping mapping);
}

package com.guru.erp.modules.procurement.returns.mapper;

import com.guru.erp.modules.procurement.returns.domain.SupplierReturn;
import com.guru.erp.modules.procurement.returns.domain.SupplierReturnLine;
import com.guru.erp.modules.procurement.returns.dto.ReturnDtos.ReturnLineResponse;
import com.guru.erp.modules.procurement.returns.dto.ReturnDtos.ReturnResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct entity→DTO mapper for the procurement returns slice (ARCHITECTURE.md §2). {@code id}
 * always maps from {@code publicId}; internal bigint ids are never exposed. The status enum flattens
 * to its wire string; embedded {@code Money} flattens to the {@code *Amount}/{@code *Currency} pair.
 * Lines are mapped via the per-line method (MapStruct auto-maps the list).
 */
@Mapper(componentModel = "spring")
public interface ReturnMapper {

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "status", expression = "java(entity.getStatus().wire())")
    @Mapping(target = "debitNoteAmount", expression = "java(entity.getDebitNote().amountMinor())")
    @Mapping(target = "debitNoteCurrency", expression = "java(entity.getDebitNote().currency())")
    @Mapping(target = "baseDebitNoteAmount", expression = "java(entity.getBaseDebitNote().amountMinor())")
    @Mapping(target = "baseDebitNoteCurrency", expression = "java(entity.getBaseDebitNote().currency())")
    ReturnResponse toResponse(SupplierReturn entity);

    @Mapping(target = "id", source = "publicId")
    ReturnLineResponse toResponse(SupplierReturnLine entity);
}

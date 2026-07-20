package com.springboot.erp.modules.settings.currency.mapper;

import com.springboot.erp.modules.settings.currency.domain.Currency;
import com.springboot.erp.modules.settings.currency.dto.CurrencyDtos.CurrencyResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct entity→DTO mapper for Currency (ARCHITECTURE.md §2). {@code id}
 * maps from {@code publicId} (internal Long id is never exposed); {@code status}
 * is the entity's derived getter.
 */
@Mapper(componentModel = "spring")
public interface CurrencyMapper {

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "isDefault", expression = "java(entity.isDefault())")
    @Mapping(target = "isActive", expression = "java(entity.isActive())")
    @Mapping(target = "status", expression = "java(entity.getStatus())")
    CurrencyResponse toResponse(Currency entity);
}

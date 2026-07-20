package com.springboot.erp.modules.settings.taxcode.mapper;

import com.springboot.erp.modules.settings.taxcode.domain.TaxCode;
import com.springboot.erp.modules.settings.taxcode.dto.TaxCodeDtos.TaxCodeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** MapStruct entity→DTO mapper for the tax code module (ARCHITECTURE.md §2). */
@Mapper(componentModel = "spring")
public interface TaxCodeMapper {

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "companyId", source = "companyPublicId")
    @Mapping(target = "status", expression = "java(taxCode.status())")
    TaxCodeResponse toResponse(TaxCode taxCode);
}

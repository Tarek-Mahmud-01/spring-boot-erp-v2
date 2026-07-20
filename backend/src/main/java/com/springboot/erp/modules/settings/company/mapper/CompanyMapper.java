package com.springboot.erp.modules.settings.company.mapper;

import com.springboot.erp.modules.settings.company.domain.Company;
import com.springboot.erp.modules.settings.company.domain.CompanyStatus;
import com.springboot.erp.modules.settings.company.dto.CompanyDtos.CompanyResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** MapStruct entity→DTO mapper for ENT-001 Company (ARCHITECTURE.md §2). */
@Mapper(componentModel = "spring")
public interface CompanyMapper {

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "primary", source = "primary")
    @Mapping(target = "status", expression = "java(statusWire(company.getStatus()))")
    CompanyResponse toResponse(Company company);

    default String statusWire(CompanyStatus status) {
        return status == null ? null : status.wire();
    }
}

package com.guru.erp.modules.settings.numbering.mapper;

import com.guru.erp.modules.settings.numbering.domain.NumberingRule;
import com.guru.erp.modules.settings.numbering.dto.NumberingRuleDtos.NumberingRuleResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** MapStruct entity-to-DTO mapper for the numbering slice (ARCHITECTURE.md §2). */
@Mapper(componentModel = "spring")
public interface NumberingRuleMapper {

    @Mapping(target = "id", source = "publicId")
    NumberingRuleResponse toResponse(NumberingRule rule);
}

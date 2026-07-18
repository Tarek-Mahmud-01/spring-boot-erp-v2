package com.guru.erp.modules.product.promotions.mapper;

import com.guru.erp.modules.product.promotions.domain.ReasonCode;
import com.guru.erp.modules.product.promotions.dto.ReasonCodeDtos.ReasonCodeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct entity→DTO mapper for {@link ReasonCode}. {@code id} maps from
 * {@code publicId}; the boolean {@code isActive} needs an explicit expression
 * mapping (record component named {@code isX}).
 */
@Mapper(componentModel = "spring")
public interface ReasonCodeMapper {

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "isActive", expression = "java(entity.isActive())")
    ReasonCodeResponse toResponse(ReasonCode entity);
}

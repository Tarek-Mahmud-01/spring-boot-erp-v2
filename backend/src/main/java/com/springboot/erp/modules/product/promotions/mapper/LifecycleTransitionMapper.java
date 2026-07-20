package com.springboot.erp.modules.product.promotions.mapper;

import com.springboot.erp.modules.product.promotions.domain.LifecycleTransition;
import com.springboot.erp.modules.product.promotions.dto.LifecycleDtos.LifecycleTransitionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct entity→DTO mapper for the {@link LifecycleTransition} ledger.
 * {@code id} maps from {@code publicId}; enum states render as their name.
 */
@Mapper(componentModel = "spring")
public interface LifecycleTransitionMapper {

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "fromState", expression = "java(entity.getFromState().name())")
    @Mapping(target = "toState", expression = "java(entity.getToState().name())")
    LifecycleTransitionResponse toResponse(LifecycleTransition entity);
}

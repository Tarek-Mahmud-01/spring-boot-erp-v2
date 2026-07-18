package com.guru.erp.modules.product.promotions.mapper;

import com.guru.erp.modules.product.promotions.domain.Promotion;
import com.guru.erp.modules.product.promotions.dto.PromotionDtos.PromotionResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct entity→DTO mapper for {@link Promotion}. {@code id} maps from
 * {@code publicId} (internal Long id is never exposed); enum {@code type} /
 * {@code status} render as their name; {@code warnings} defaults to empty for
 * reads (create supplies advisories separately).
 */
@Mapper(componentModel = "spring")
public interface PromotionMapper {

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "type", expression = "java(entity.getType().name())")
    @Mapping(target = "status", expression = "java(entity.getStatus().name())")
    @Mapping(target = "stackable", expression = "java(entity.isStackable())")
    @Mapping(target = "reasonRequired", expression = "java(entity.isReasonRequired())")
    @Mapping(target = "warnings", expression = "java(java.util.List.<String>of())")
    PromotionResponse toResponse(Promotion entity);

    default PromotionResponse toResponse(Promotion entity, List<String> warnings) {
        PromotionResponse base = toResponse(entity);
        return new PromotionResponse(
            base.id(), base.companyId(), base.name(), base.type(), base.config(),
            base.scope(), base.segmentId(), base.dateFrom(), base.dateTo(),
            base.stackable(), base.reasonRequired(), base.status(), base.version(),
            base.createdAt(), base.updatedAt(),
            warnings == null ? List.of() : warnings);
    }
}

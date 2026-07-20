package com.springboot.erp.modules.product.pricing.mapper;

import com.springboot.erp.modules.product.pricing.domain.PriceHistory;
import com.springboot.erp.modules.product.pricing.domain.PriceList;
import com.springboot.erp.modules.product.pricing.domain.PriceListItem;
import com.springboot.erp.modules.product.pricing.domain.ProductLocationOverride;
import com.springboot.erp.modules.product.pricing.dto.PricingDtos.LocationOverrideResponse;
import com.springboot.erp.modules.product.pricing.dto.PricingDtos.PriceHistoryResponse;
import com.springboot.erp.modules.product.pricing.dto.PricingDtos.PriceListItemResponse;
import com.springboot.erp.modules.product.pricing.dto.PricingDtos.PriceListResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct entity→DTO mappers for the pricing slice. Every {@code id} maps from
 * the entity's {@code publicId}; internal Long ids are never exposed. Money is
 * flattened from the embedded {@link com.springboot.erp.platform.money.Money} into
 * {@code priceAmount} (minor units) + {@code priceCurrency} via explicit
 * expressions.
 */
@Mapper(componentModel = "spring")
public interface PricingMapper {

    @Mapping(target = "id", source = "publicId")
    PriceListResponse toResponse(PriceList entity);

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "priceListId", expression = "java(entity.getPriceList().getPublicId())")
    @Mapping(target = "priceAmount", expression = "java(entity.getPrice().amountMinor())")
    @Mapping(target = "priceCurrency", expression = "java(entity.getPrice().currency())")
    PriceListItemResponse toResponse(PriceListItem entity);

    @Mapping(target = "id", source = "publicId")
    PriceHistoryResponse toResponse(PriceHistory entity);

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "priceAmount", expression = "java(entity.getPrice().amountMinor())")
    @Mapping(target = "priceCurrency", expression = "java(entity.getPrice().currency())")
    LocationOverrideResponse toResponse(ProductLocationOverride entity);
}

package com.guru.erp.modules.product.catalog.mapper;

import com.guru.erp.modules.product.catalog.domain.CustomAttributeValue;
import com.guru.erp.modules.product.catalog.domain.Product;
import com.guru.erp.modules.product.catalog.domain.ProductBarcode;
import com.guru.erp.modules.product.catalog.domain.ProductImage;
import com.guru.erp.modules.product.catalog.domain.ProductVariant;
import com.guru.erp.modules.product.catalog.dto.BarcodeDtos.BarcodeResponse;
import com.guru.erp.modules.product.catalog.dto.CustomAttributeDtos.CustomAttributeResponse;
import com.guru.erp.modules.product.catalog.dto.ImageDtos.ImageResponse;
import com.guru.erp.modules.product.catalog.dto.ProductDtos.ProductResponse;
import com.guru.erp.modules.product.catalog.dto.VariantDtos.VariantResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct entity→DTO mappers for the catalog slice (ARCHITECTURE.md §2).
 * {@code id} always maps from {@code publicId}; internal bigint ids are never
 * exposed. Money is embedded, so its {@code amountMinor}/{@code currency} pair is
 * flattened into the {@code *Amount}/{@code *Currency} response fields via
 * explicit expressions. Boolean {@code isX} components use the boolean getter.
 *
 * <p>Cross-slice / cross-entity public-id fields the mapper cannot derive from
 * the entity alone (productId/variantId built from internal FKs, lifecycleState
 * enum→string) are set by the service after mapping via the {@code with*} helpers.
 */
@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "costAmount", expression = "java(entity.getCost().amountMinor())")
    @Mapping(target = "costCurrency", expression = "java(entity.getCost().currency())")
    @Mapping(target = "sellAmount", expression = "java(entity.getSell().amountMinor())")
    @Mapping(target = "sellCurrency", expression = "java(entity.getSell().currency())")
    @Mapping(target = "currentCostAmount", expression = "java(entity.getCurrentCostAmount())")
    @Mapping(target = "hasVariants", expression = "java(entity.isHasVariants())")
    @Mapping(target = "isActive", expression = "java(entity.isActive())")
    @Mapping(target = "lifecycleState", expression = "java(entity.getLifecycleState().name())")
    @Mapping(target = "restrictionAge18", expression = "java(entity.isRestrictionAge18())")
    @Mapping(target = "restrictionAge21", expression = "java(entity.isRestrictionAge21())")
    @Mapping(target = "restrictionControlledDisplay",
        expression = "java(entity.isRestrictionControlledDisplay())")
    @Mapping(target = "soldByWeight", expression = "java(entity.isSoldByWeight())")
    ProductResponse toResponse(Product entity);

    // Variant: productId (parent publicId) is resolved by the service and passed in.
    @Mapping(target = "id", source = "entity.publicId")
    @Mapping(target = "productId", source = "productPublicId")
    @Mapping(target = "costAmount", expression = "java(entity.getCost().amountMinor())")
    @Mapping(target = "costCurrency", expression = "java(entity.getCost().currency())")
    @Mapping(target = "sellAmount", expression = "java(entity.getSell().amountMinor())")
    @Mapping(target = "sellCurrency", expression = "java(entity.getSell().currency())")
    @Mapping(target = "currentCostAmount", expression = "java(entity.getCurrentCostAmount())")
    VariantResponse toResponse(ProductVariant entity, String productPublicId);

    @Mapping(target = "id", source = "entity.publicId")
    @Mapping(target = "productId", source = "productPublicId")
    @Mapping(target = "variantId", source = "variantPublicId")
    @Mapping(target = "format", expression = "java(entity.getFormat().name())")
    @Mapping(target = "isPrimary", expression = "java(entity.isPrimary())")
    BarcodeResponse toResponse(ProductBarcode entity, String productPublicId, String variantPublicId);

    @Mapping(target = "id", source = "entity.publicId")
    @Mapping(target = "productId", source = "productPublicId")
    @Mapping(target = "fileId", source = "entity.fileId")
    @Mapping(target = "position", source = "entity.position")
    @Mapping(target = "bytes", source = "entity.bytes")
    ImageResponse toResponse(ProductImage entity, String productPublicId);

    @Mapping(target = "id", source = "entity.publicId")
    @Mapping(target = "productId", source = "productPublicId")
    @Mapping(target = "variantId", source = "variantPublicId")
    CustomAttributeResponse toResponse(
        CustomAttributeValue entity, String productPublicId, String variantPublicId);
}

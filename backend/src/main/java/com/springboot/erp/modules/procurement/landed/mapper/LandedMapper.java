package com.springboot.erp.modules.procurement.landed.mapper;

import com.springboot.erp.modules.procurement.landed.domain.LabelSize;
import com.springboot.erp.modules.procurement.landed.domain.LandedCost;
import com.springboot.erp.modules.procurement.landed.domain.LandedCostAllocation;
import com.springboot.erp.modules.procurement.landed.domain.UnitBarcode;
import com.springboot.erp.modules.procurement.landed.domain.UnitBarcodeItem;
import com.springboot.erp.modules.procurement.landed.dto.LandedCostDtos.LandedCostAllocationResponse;
import com.springboot.erp.modules.procurement.landed.dto.LandedCostDtos.LandedCostResponse;
import com.springboot.erp.modules.procurement.landed.dto.UnitBarcodeDtos.LabelSizeResponse;
import com.springboot.erp.modules.procurement.landed.dto.UnitBarcodeDtos.UnitBarcodeItemResponse;
import com.springboot.erp.modules.procurement.landed.dto.UnitBarcodeDtos.UnitBarcodeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct entity→DTO mappers for the procurement landed slice (ARCHITECTURE.md §2). {@code id}
 * always maps from {@code publicId}; internal bigint ids are never exposed. Enums flatten to their
 * wire string; embedded {@code Money} flattens to the {@code *Amount}/{@code *Currency} pair. Boolean
 * {@code isX} record components need an explicit expression mapping.
 */
@Mapper(componentModel = "spring")
public interface LandedMapper {

    // --- Landed cost ---

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "chargeType", expression = "java(entity.getChargeType().name())")
    @Mapping(target = "amount", expression = "java(entity.getAmount().amountMinor())")
    @Mapping(target = "currency", expression = "java(entity.getAmount().currency())")
    @Mapping(target = "baseAmount", expression = "java(entity.getBaseAmount().amountMinor())")
    @Mapping(target = "baseCurrency", expression = "java(entity.getBaseAmount().currency())")
    @Mapping(target = "allocationBasis", expression = "java(entity.getAllocationBasis().name())")
    @Mapping(target = "status", expression = "java(entity.getStatus().name())")
    LandedCostResponse toResponse(LandedCost entity);

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "allocatedAmount", expression = "java(entity.getAllocatedAmount().amountMinor())")
    @Mapping(target = "allocatedCurrency", expression = "java(entity.getAllocatedAmount().currency())")
    LandedCostAllocationResponse toResponse(LandedCostAllocation entity);

    // --- Unit barcode ---

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "barcodeFormat", expression = "java(entity.getBarcodeFormat().name())")
    @Mapping(target = "bundle", expression = "java(entity.isBundle())")
    @Mapping(target = "status", expression = "java(entity.getStatus().wire())")
    UnitBarcodeResponse toResponse(UnitBarcode entity);

    @Mapping(target = "id", source = "publicId")
    UnitBarcodeItemResponse toResponse(UnitBarcodeItem entity);

    // --- Label size ---

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "isDefault", expression = "java(entity.isDefault())")
    @Mapping(target = "active", expression = "java(entity.isActive())")
    LabelSizeResponse toResponse(LabelSize entity);
}

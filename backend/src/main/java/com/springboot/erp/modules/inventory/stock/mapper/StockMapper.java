package com.springboot.erp.modules.inventory.stock.mapper;

import com.springboot.erp.modules.inventory.stock.domain.InventoryValuationConfig;
import com.springboot.erp.modules.inventory.stock.domain.ProductBatch;
import com.springboot.erp.modules.inventory.stock.domain.StockLedger;
import com.springboot.erp.modules.inventory.stock.dto.ProductBatchDtos.BatchResponse;
import com.springboot.erp.modules.inventory.stock.dto.StockLedgerDtos.LedgerEntryResponse;
import com.springboot.erp.modules.inventory.stock.dto.ValuationConfigDtos.ValuationConfigResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct entity→DTO mappers for the inventory "stock" slice (ARCHITECTURE.md
 * §2). {@code id} always maps from {@code publicId}; internal bigint ids are
 * never exposed. Embedded {@link com.springboot.erp.platform.money.Money} pairs are
 * flattened into {@code *Amount}/{@code *Currency} via explicit expressions.
 * Boolean {@code isX} components use the boolean getter.
 */
@Mapper(componentModel = "spring")
public interface StockMapper {

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "unitCostAmount", expression = "java(entity.getUnitCost().amountMinor())")
    @Mapping(target = "unitCostCurrency", expression = "java(entity.getUnitCost().currency())")
    LedgerEntryResponse toResponse(StockLedger entity);

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "locked", expression = "java(entity.isLocked())")
    ValuationConfigResponse toResponse(InventoryValuationConfig entity);

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "grnCostAmount", expression = "java(entity.getGrnCost().amountMinor())")
    @Mapping(target = "grnCostCurrency", expression = "java(entity.getGrnCost().currency())")
    BatchResponse toResponse(ProductBatch entity);
}

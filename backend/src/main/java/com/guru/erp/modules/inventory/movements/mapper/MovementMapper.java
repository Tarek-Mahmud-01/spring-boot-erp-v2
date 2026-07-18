package com.guru.erp.modules.inventory.movements.mapper;

import com.guru.erp.modules.inventory.movements.domain.StockAdjustment;
import com.guru.erp.modules.inventory.movements.domain.StockAdjustmentLine;
import com.guru.erp.modules.inventory.movements.domain.StockTransfer;
import com.guru.erp.modules.inventory.movements.domain.StockTransferLine;
import com.guru.erp.modules.inventory.movements.dto.AdjustmentDtos.AdjustmentLineResponse;
import com.guru.erp.modules.inventory.movements.dto.AdjustmentDtos.AdjustmentResponse;
import com.guru.erp.modules.inventory.movements.dto.TransferDtos.TransferLineResponse;
import com.guru.erp.modules.inventory.movements.dto.TransferDtos.TransferResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct entity→DTO mappers for the inventory movements slice (ARCHITECTURE.md §2). {@code id}
 * always maps from {@code publicId}; internal bigint ids are never exposed. Status enums flatten
 * to their wire string; embedded {@code Money} flattens to the {@code *Amount}/{@code *Currency}
 * pair. Lines are mapped via the per-line methods (MapStruct auto-maps the list).
 */
@Mapper(componentModel = "spring")
public interface MovementMapper {

    // --- Transfer ---

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "status", expression = "java(entity.getStatus().wire())")
    TransferResponse toResponse(StockTransfer entity);

    @Mapping(target = "id", source = "publicId")
    TransferLineResponse toResponse(StockTransferLine entity);

    // --- Adjustment ---

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "status", expression = "java(entity.getStatus().wire())")
    @Mapping(target = "thresholdExceeded", expression = "java(entity.isThresholdExceeded())")
    AdjustmentResponse toResponse(StockAdjustment entity);

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "unitCostAmount", expression = "java(entity.getUnitCost().amountMinor())")
    @Mapping(target = "unitCostCurrency", expression = "java(entity.getUnitCost().currency())")
    AdjustmentLineResponse toResponse(StockAdjustmentLine entity);
}

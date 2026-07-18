package com.guru.erp.modules.inventory.counts.mapper;

import com.guru.erp.modules.inventory.counts.domain.BarcodeNomenclatureRule;
import com.guru.erp.modules.inventory.counts.domain.CycleCountLine;
import com.guru.erp.modules.inventory.counts.domain.CycleCountPlan;
import com.guru.erp.modules.inventory.counts.domain.StockOpening;
import com.guru.erp.modules.inventory.counts.dto.BarcodeRuleDtos.BarcodeRuleResponse;
import com.guru.erp.modules.inventory.counts.dto.CycleCountDtos.CycleCountLineResponse;
import com.guru.erp.modules.inventory.counts.dto.CycleCountDtos.CycleCountResponse;
import com.guru.erp.modules.inventory.counts.dto.StockOpeningDtos.StockOpeningResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct entity→DTO mappers for the inventory counts slice (ARCHITECTURE.md
 * §2). {@code id} always maps from {@code publicId}; internal bigint ids are
 * never exposed. Status enums map to their wire string; boolean {@code isX}
 * components use the boolean getter via explicit expressions.
 */
@Mapper(componentModel = "spring")
public interface CountsMapper {

    // --- Cycle count --------------------------------------------------------

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "status", expression = "java(plan.getStatus().wire())")
    CycleCountResponse toResponse(CycleCountPlan plan);

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "requiresRecount", expression = "java(line.isRequiresRecount())")
    CycleCountLineResponse toResponse(CycleCountLine line);

    // --- Stock opening ------------------------------------------------------

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "status", expression = "java(opening.getStatus().wire())")
    @Mapping(target = "openingTotalValue", expression = "java(openingTotalValue(opening))")
    StockOpeningResponse toResponse(StockOpening opening);

    /**
     * Opening valuation (minor units) = openingQty × unitCost, HALF_EVEN — the
     * amount booked to the GL. Backend-owned; mirrors the reference
     * {@code opening_total_value} computed field.
     */
    default long openingTotalValue(StockOpening opening) {
        BigDecimal qty = opening.getOpeningQty() == null ? BigDecimal.ZERO : opening.getOpeningQty();
        return qty.multiply(BigDecimal.valueOf(opening.getUnitCostAmount()))
            .setScale(0, RoundingMode.HALF_EVEN)
            .longValueExact();
    }

    // --- Barcode nomenclature rule -----------------------------------------

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "isActive", expression = "java(rule.isActive())")
    BarcodeRuleResponse toResponse(BarcodeNomenclatureRule rule);
}

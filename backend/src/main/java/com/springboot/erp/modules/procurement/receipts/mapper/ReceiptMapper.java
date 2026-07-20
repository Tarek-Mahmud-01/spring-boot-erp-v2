package com.springboot.erp.modules.procurement.receipts.mapper;

import com.springboot.erp.modules.procurement.receipts.domain.GoodsReceipt;
import com.springboot.erp.modules.procurement.receipts.domain.GoodsReceiptLine;
import com.springboot.erp.modules.procurement.receipts.dto.ReceiptDtos.GrnLineResponse;
import com.springboot.erp.modules.procurement.receipts.dto.ReceiptDtos.GrnResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct entity→DTO mappers for the receipts slice (ARCHITECTURE.md §2). {@code id} always maps
 * from {@code publicId}; internal bigint ids are never exposed. The status enum flattens to its
 * wire string; the discrepancy enum flattens likewise; the optional embedded {@code Money} pairs
 * (mrp / sellPrice) flatten to nullable {@code *Amount}/{@code *Currency} pairs via helper methods.
 * Lines are mapped via the per-line method (MapStruct auto-maps the list).
 */
@Mapper(componentModel = "spring")
public interface ReceiptMapper {

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "status", expression = "java(entity.getStatus().wire())")
    GrnResponse toResponse(GoodsReceipt entity);

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "discrepancyType",
        expression = "java(entity.getDiscrepancyType() == null ? null : entity.getDiscrepancyType().wire())")
    @Mapping(target = "mrpAmount", expression = "java(amount(entity.getMrp()))")
    @Mapping(target = "mrpCurrency", expression = "java(currency(entity.getMrp()))")
    @Mapping(target = "sellPriceAmount", expression = "java(amount(entity.getSellPrice()))")
    @Mapping(target = "sellPriceCurrency", expression = "java(currency(entity.getSellPrice()))")
    GrnLineResponse toResponse(GoodsReceiptLine entity);

    /** Null-safe minor-units accessor for an optional embedded Money. */
    default Long amount(com.springboot.erp.platform.money.Money m) {
        return m == null ? null : m.amountMinor();
    }

    /** Null-safe currency accessor for an optional embedded Money. */
    default String currency(com.springboot.erp.platform.money.Money m) {
        return m == null ? null : m.currency();
    }
}

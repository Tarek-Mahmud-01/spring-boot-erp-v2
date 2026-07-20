package com.springboot.erp.modules.procurement.orders.mapper;

import com.springboot.erp.modules.procurement.orders.domain.PurchaseOrder;
import com.springboot.erp.modules.procurement.orders.domain.PurchaseOrderLine;
import com.springboot.erp.modules.procurement.orders.domain.PurchaseOrderVersion;
import com.springboot.erp.modules.procurement.orders.domain.PurchaseRequisition;
import com.springboot.erp.modules.procurement.orders.domain.PurchaseRequisitionLine;
import com.springboot.erp.modules.procurement.orders.dto.PoDtos.PoLineResponse;
import com.springboot.erp.modules.procurement.orders.dto.PoDtos.PoResponse;
import com.springboot.erp.modules.procurement.orders.dto.PoDtos.PoVersionResponse;
import com.springboot.erp.modules.procurement.orders.dto.PrDtos.PrLineResponse;
import com.springboot.erp.modules.procurement.orders.dto.PrDtos.PrResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct entity→DTO mappers for the procurement orders slice (ARCHITECTURE.md §2). {@code id}
 * always maps from {@code publicId}; internal bigint ids are never exposed. Status enums flatten to
 * their wire string; embedded {@link com.springboot.erp.platform.money.Money} flattens to the
 * {@code *Amount}/{@code *Currency} pair. The boolean {@code isDirect} record component needs an
 * explicit expression (MapStruct's getter probing does not match the record accessor name).
 */
@Mapper(componentModel = "spring")
public interface OrdersMapper {

    // --- Purchase Requisition ---

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "status", expression = "java(entity.getStatus().wire())")
    PrResponse toResponse(PurchaseRequisition entity);

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "unitPriceAmount", expression = "java(entity.getUnitPrice().amountMinor())")
    @Mapping(target = "unitPriceCurrency", expression = "java(entity.getUnitPrice().currency())")
    @Mapping(target = "status", expression = "java(entity.getStatus().wire())")
    PrLineResponse toResponse(PurchaseRequisitionLine entity);

    // --- Purchase Order ---

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "status", expression = "java(entity.getStatus().wire())")
    @Mapping(target = "isDirect", expression = "java(entity.isDirect())")
    PoResponse toResponse(PurchaseOrder entity);

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "unitPriceAmount", expression = "java(entity.getUnitPrice().amountMinor())")
    @Mapping(target = "unitPriceCurrency", expression = "java(entity.getUnitPrice().currency())")
    @Mapping(target = "lineTotalAmount", expression = "java(entity.getLineTotal().amountMinor())")
    @Mapping(target = "lineTotalCurrency", expression = "java(entity.getLineTotal().currency())")
    @Mapping(target = "lineStatus", expression = "java(lineStatus(entity))")
    PoLineResponse toResponse(PurchaseOrderLine entity);

    @Mapping(target = "id", source = "publicId")
    PoVersionResponse toResponse(PurchaseOrderVersion entity);

    /**
     * Derived receipt status for a PO line (reference {@code POLineResponse.line_status}):
     * "Fully Received" when received ≥ ordered, "Partially Received" when some received, else
     * "Draft".
     */
    default String lineStatus(PurchaseOrderLine line) {
        java.math.BigDecimal received = line.getQtyReceivedTotal();
        java.math.BigDecimal ordered = line.getQtyOrdered();
        if (received.compareTo(ordered) >= 0) {
            return "Fully Received";
        }
        if (received.signum() > 0) {
            return "Partially Received";
        }
        return "Draft";
    }
}

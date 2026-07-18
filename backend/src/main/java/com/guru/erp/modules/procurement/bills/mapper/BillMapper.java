package com.guru.erp.modules.procurement.bills.mapper;

import com.guru.erp.modules.procurement.bills.domain.AmountReceived;
import com.guru.erp.modules.procurement.bills.domain.SupplierBill;
import com.guru.erp.modules.procurement.bills.domain.SupplierBillLine;
import com.guru.erp.modules.procurement.bills.domain.SupplierPayment;
import com.guru.erp.modules.procurement.bills.domain.SupplierPaymentTender;
import com.guru.erp.modules.procurement.bills.dto.AmountReceivedDtos.AmountReceivedResponse;
import com.guru.erp.modules.procurement.bills.dto.BillDtos.BillLineResponse;
import com.guru.erp.modules.procurement.bills.dto.BillDtos.BillResponse;
import com.guru.erp.modules.procurement.bills.dto.PaymentDtos.PaymentResponse;
import com.guru.erp.modules.procurement.bills.dto.PaymentDtos.TenderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct entity→DTO mappers for the procurement bills slice. {@code id} always maps from
 * {@code publicId}; internal bigint ids are never exposed. Status enums flatten to their wire
 * string. The {@code isCapitalItem} record component needs an explicit expression (MapStruct does
 * not match the {@code isX} getter to a same-named target automatically). The bill's GRN/PO link
 * ids and the payment's tenders are projected via explicit expressions.
 */
@Mapper(componentModel = "spring")
public interface BillMapper {

    // --- Supplier Bill ---

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "status", expression = "java(entity.getStatus().wire())")
    @Mapping(target = "grnIds",
        expression = "java(entity.getGrnLinks().stream().map(l -> l.getGrnId()).toList())")
    @Mapping(target = "poIds",
        expression = "java(entity.getPoLinks().stream().map(l -> l.getPoId()).toList())")
    BillResponse toResponse(SupplierBill entity);

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "isCapitalItem", expression = "java(entity.isCapitalItem())")
    BillLineResponse toResponse(SupplierBillLine entity);

    // --- Supplier Payment ---

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "status", expression = "java(entity.getStatus().wire())")
    PaymentResponse toResponse(SupplierPayment entity);

    @Mapping(target = "id", source = "publicId")
    TenderResponse toResponse(SupplierPaymentTender entity);

    // --- Amount Received ---

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "status", expression = "java(entity.getStatus().wire())")
    AmountReceivedResponse toResponse(AmountReceived entity);
}

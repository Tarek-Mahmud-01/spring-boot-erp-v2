package com.springboot.erp.modules.procurement.suppliers.mapper;

import com.springboot.erp.modules.procurement.suppliers.domain.Supplier;
import com.springboot.erp.modules.procurement.suppliers.domain.SupplierAttachment;
import com.springboot.erp.modules.procurement.suppliers.domain.SupplierStatus;
import com.springboot.erp.modules.procurement.suppliers.dto.SupplierDtos.SupplierAttachmentResponse;
import com.springboot.erp.modules.procurement.suppliers.dto.SupplierDtos.SupplierResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * MapStruct entity→DTO mappers for the procurement suppliers slice (ARCHITECTURE.md §2). {@code id}
 * maps from {@code publicId}; internal bigint ids are never exposed. The {@link SupplierStatus}
 * enum flattens to its wire label; the embedded {@code Money} credit limit / opening balance flatten
 * to their {@code *Amount}/{@code *Currency} pairs; the pill tone is backend-owned (reference M-2).
 */
@Mapper(componentModel = "spring")
public interface SupplierMapper {

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "type", expression = "java(entity.getType().name())")
    @Mapping(target = "creditLimitAmount", expression = "java(entity.getCreditLimit().amountMinor())")
    @Mapping(target = "creditLimitCurrency", expression = "java(entity.getCreditLimit().currency())")
    @Mapping(target = "openingBalanceAmount", expression = "java(entity.getOpeningBalance().amountMinor())")
    @Mapping(target = "openingBalanceCurrency", expression = "java(entity.getOpeningBalance().currency())")
    @Mapping(target = "status", expression = "java(entity.getStatus().wire())")
    @Mapping(target = "displayStatusTone", source = "status", qualifiedByName = "tone")
    SupplierResponse toResponse(Supplier entity);

    @Mapping(target = "id", source = "publicId")
    SupplierAttachmentResponse toResponse(SupplierAttachment entity);

    /** Backend-owned pill tone (reference _SUPPLIER_STATUS_TONE). */
    @Named("tone")
    default String tone(SupplierStatus status) {
        if (status == null) {
            return "draft";
        }
        return switch (status) {
            case ACTIVE -> "success";
            case BLOCKED -> "danger";
            case INACTIVE -> "draft";
        };
    }
}

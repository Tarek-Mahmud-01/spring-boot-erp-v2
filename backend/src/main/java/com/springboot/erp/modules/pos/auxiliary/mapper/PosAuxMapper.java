package com.springboot.erp.modules.pos.auxiliary.mapper;

import com.springboot.erp.modules.pos.auxiliary.domain.PosEvent;
import com.springboot.erp.modules.pos.auxiliary.domain.PosRefund;
import com.springboot.erp.modules.pos.auxiliary.dto.PosEventDtos.PosEventResponse;
import com.springboot.erp.modules.pos.auxiliary.dto.RefundDtos.RefundResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct entity to DTO mappers for the POS "aux" slice (ARCHITECTURE.md §2). {@code id} always
 * maps from {@code publicId}; internal bigint ids are never exposed. Enums flatten to their wire
 * string; the embedded {@code Money} on {@link PosRefund} flattens to the amount/currency pair.
 *
 * <p>{@link com.springboot.erp.modules.pos.auxiliary.domain.PosParkedSale} has no direct mapper method — its
 * response is a cross-aggregate summary (line count / total pulled from the referenced
 * transaction) assembled by the service, not a 1:1 entity projection.
 */
@Mapper(componentModel = "spring")
public interface PosAuxMapper {

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "mode", expression = "java(entity.getMode().name())")
    @Mapping(target = "pricedFrom", expression = "java(entity.getPricedFrom() == null ? null : entity.getPricedFrom().name())")
    @Mapping(target = "managerApproved", expression = "java(entity.isManagerApproved())")
    @Mapping(target = "managerApprovalMethod", expression = "java(entity.getManagerApprovalMethod() == null ? null : entity.getManagerApprovalMethod().name())")
    @Mapping(target = "totalRefundAmount", expression = "java(entity.getTotalRefund().amountMinor())")
    @Mapping(target = "currency", expression = "java(entity.getTotalRefund().currency())")
    RefundResponse toResponse(PosRefund entity);

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "type", expression = "java(entity.getType().name())")
    PosEventResponse toResponse(PosEvent entity);
}

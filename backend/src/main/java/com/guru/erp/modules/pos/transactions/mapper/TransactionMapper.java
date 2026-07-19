package com.guru.erp.modules.pos.transactions.mapper;

import com.guru.erp.modules.pos.transactions.domain.PosTender;
import com.guru.erp.modules.pos.transactions.domain.PosTransaction;
import com.guru.erp.modules.pos.transactions.domain.PosTransactionLine;
import com.guru.erp.modules.pos.transactions.dto.TransactionDtos.PosLineResponse;
import com.guru.erp.modules.pos.transactions.dto.TransactionDtos.PosTenderResponse;
import com.guru.erp.modules.pos.transactions.dto.TransactionDtos.PosTransactionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct entity→DTO mappers for the POS transactions slice (ARCHITECTURE.md §2). {@code id}
 * always maps from {@code publicId}; internal bigint ids are never exposed. Status/type enums
 * flatten to their wire name; {@code is}-prefixed boolean record components need an explicit
 * expression (MapStruct's getter probing does not reliably match the record accessor name).
 *
 * <p>{@code balanceAmount} and {@code ageRequired} are derived, not persisted columns, so the
 * header mapping takes them as extra parameters (computed by the service, which alone knows the
 * active-tender set and the age-restriction policy).
 */
@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "isRestricted18", expression = "java(entity.isRestricted18())")
    @Mapping(target = "isRestricted21", expression = "java(entity.isRestricted21())")
    @Mapping(target = "isRestrictedControlledDisplay", expression = "java(entity.isRestrictedControlledDisplay())")
    PosLineResponse toResponse(PosTransactionLine entity);

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "isReversed", expression = "java(entity.isReversed())")
    PosTenderResponse toResponse(PosTender entity);

    /** Full header + lines + tenders response, with the two derived fields supplied by the caller. */
    default PosTransactionResponse toResponse(PosTransaction entity, long balanceAmount, boolean ageRequired) {
        return new PosTransactionResponse(
            entity.getPublicId(),
            entity.getRegisterId(),
            entity.getLocationId(),
            entity.getCustomerId(),
            entity.getType().name(),
            entity.getStatus().name(),
            entity.getReceiptNumber(),
            entity.getDocumentType(),
            entity.getSubtotalAmount(),
            entity.getTaxAmount(),
            entity.getDiscountAmount(),
            entity.getManualDiscountAmount(),
            entity.getManagerApprovalName(),
            entity.getSurchargeAmount(),
            entity.getSurchargeTaxAmount(),
            entity.isSurchargeTaxable(),
            entity.getSurchargeLabel(),
            entity.getTotalAmount(),
            entity.getPaidAmount(),
            entity.getChangeAmount(),
            balanceAmount,
            entity.getCurrency(),
            entity.isAgeVerified(),
            ageRequired,
            entity.getAgeIdType(),
            entity.getReprintCount(),
            entity.getRefundOfId(),
            entity.getOccurredAt(),
            entity.getCompletedAt(),
            entity.getVersion(),
            entity.getLines().stream().map(this::toResponse).toList(),
            entity.getTenders().stream().map(this::toResponse).toList());
    }
}

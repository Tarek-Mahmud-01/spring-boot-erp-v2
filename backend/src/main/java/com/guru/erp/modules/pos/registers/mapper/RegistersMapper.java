package com.guru.erp.modules.pos.registers.mapper;

import com.guru.erp.modules.pos.registers.domain.PosTillMovement;
import com.guru.erp.modules.pos.registers.domain.PosTillSession;
import com.guru.erp.modules.pos.registers.domain.Register;
import com.guru.erp.modules.pos.registers.domain.RegisterPeripheral;
import com.guru.erp.modules.pos.registers.dto.RegisterDtos.PeripheralResponse;
import com.guru.erp.modules.pos.registers.dto.RegisterDtos.RegisterResponse;
import com.guru.erp.modules.pos.registers.dto.TillDtos.TillMovementResponse;
import com.guru.erp.modules.pos.registers.dto.TillDtos.TillSessionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct entity->DTO mappers for the pos.registers slice (ARCHITECTURE.md
 * §2). {@code id} always maps from {@code publicId}; internal bigint ids are
 * never exposed. Status/enum columns flatten to their wire string; embedded
 * {@code Money} flattens to the {@code *Amount}/{@code currency} pair.
 */
@Mapper(componentModel = "spring")
public interface RegistersMapper {

    // --- Register / RegisterPeripheral ---

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "operatingMode", expression = "java(entity.getOperatingMode().name())")
    @Mapping(target = "status", expression = "java(entity.getStatus().wire())")
    RegisterResponse toResponse(Register entity);

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "registerId", source = "register.publicId")
    @Mapping(target = "type", expression = "java(entity.getType().name())")
    @Mapping(target = "connection", expression = "java(entity.getConnection().name())")
    @Mapping(target = "enabled", expression = "java(entity.isEnabled())")
    PeripheralResponse toResponse(RegisterPeripheral entity);

    // --- PosTillSession / PosTillMovement ---

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "registerId", source = "register.publicId")
    @Mapping(target = "status", expression = "java(entity.getStatus().name())")
    @Mapping(target = "openingFloatAmount", expression = "java(entity.getOpeningFloat().amountMinor())")
    @Mapping(target = "expectedCashAmount", expression = "java(entity.getExpectedCash() == null ? null : entity.getExpectedCash().amountMinor())")
    @Mapping(target = "countedCashAmount", expression = "java(entity.getCountedCash() == null ? null : entity.getCountedCash().amountMinor())")
    @Mapping(target = "currency", expression = "java(entity.getOpeningFloat().currency())")
    @Mapping(target = "varianceApproved", expression = "java(entity.getVarianceApprovedBy() != null)")
    @Mapping(target = "movements", source = "movements")
    TillSessionResponse toResponse(PosTillSession entity);

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "type", expression = "java(entity.getType().name())")
    @Mapping(target = "amount", expression = "java(entity.getAmount().amountMinor())")
    @Mapping(target = "currency", expression = "java(entity.getAmount().currency())")
    @Mapping(target = "createdBy", source = "createdBy")
    TillMovementResponse toResponse(PosTillMovement entity);
}

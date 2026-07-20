package com.springboot.erp.modules.settings.location.mapper;

import com.springboot.erp.modules.settings.location.domain.Address;
import com.springboot.erp.modules.settings.location.domain.Location;
import com.springboot.erp.modules.settings.location.dto.LocationDtos.AddressRequest;
import com.springboot.erp.modules.settings.location.dto.LocationDtos.AddressResponse;
import com.springboot.erp.modules.settings.location.dto.LocationDtos.LocationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct entity↔DTO mapper for the location slice (ARCHITECTURE.md §2).
 * Exposes {@code publicId} as {@code id} and {@code companyPublicId} as
 * {@code companyId}; the internal bigint id never leaves the service.
 */
@Mapper(componentModel = "spring")
public interface LocationMapper {

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "companyId", source = "company.publicId")
    LocationResponse toResponse(Location location);

    AddressResponse toResponse(Address address);

    /** Convert a validated address payload into the persistable value object. */
    Address toAddress(AddressRequest request);
}

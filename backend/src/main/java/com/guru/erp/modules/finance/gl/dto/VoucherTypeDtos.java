package com.guru.erp.modules.finance.gl.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Wire DTOs for {@code VoucherType} (reference {@code VoucherTypeCreateRequest}/{@code Response}). */
public final class VoucherTypeDtos {

    private VoucherTypeDtos() {
    }

    public record VoucherTypeCreateRequest(
        @NotNull @Size(max = 10) String code,
        @NotNull @Size(max = 8) String prefix,
        @NotNull @Size(max = 80) String name,
        @Size(max = 500) String description,
        boolean operational,
        @Size(max = 40) String operationalModule
    ) {
    }

    public record VoucherTypeUpdateRequest(
        @NotNull long version,
        String name,
        String description,
        String prefix,
        Boolean active
    ) {
    }

    public record VoucherTypeResponse(
        String id,
        String code,
        String prefix,
        String name,
        String description,
        boolean operational,
        String operationalModule,
        boolean active,
        long version
    ) {
    }
}

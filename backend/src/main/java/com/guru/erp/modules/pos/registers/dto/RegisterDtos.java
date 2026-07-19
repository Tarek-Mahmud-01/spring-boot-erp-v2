package com.guru.erp.modules.pos.registers.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Wire DTOs for ENT-060 Register + ENT-060a RegisterPeripheral (records —
 * ARCHITECTURE.md §2). Mirrors the reference Pydantic schemas
 * ({@code RegisterCreateRequest}/{@code RegisterUpdateRequest}/
 * {@code RegisterResponse}/{@code PeripheralPayload}/{@code PeripheralResponse}).
 */
public final class RegisterDtos {

    private RegisterDtos() {
    }

    /** One peripheral spec on register create, or a standalone bind request. */
    public record PeripheralPayload(
        @NotBlank String type,
        @NotBlank String connection,
        Map<String, Object> config,
        boolean enabled
    ) {
    }

    /** POST /api/pos/registers body. */
    public record RegisterCreateRequest(
        @NotNull @Size(min = 26, max = 26) String locationId,
        @NotBlank @Size(min = 1, max = 20) String code,
        @NotBlank @Size(min = 1, max = 100) String displayName,
        String operatingMode,
        @Valid List<PeripheralPayload> peripherals
    ) {
    }

    /** PATCH /api/pos/registers/{id} body — partial update. */
    public record RegisterUpdateRequest(
        @Size(min = 1, max = 20) String code,
        @Size(min = 1, max = 100) String displayName,
        String operatingMode,
        String status,
        Long version
    ) {
    }

    /** POST /api/pos/registers/{id}/peripherals body — binds (or replaces) a peripheral by type. */
    public record PeripheralBindRequest(
        @NotBlank String type,
        @NotBlank String connection,
        Map<String, Object> config,
        boolean enabled
    ) {
    }

    /** PATCH .../peripherals/{peripheralId} body. */
    public record PeripheralUpdateRequest(
        String connection,
        Map<String, Object> config,
        Boolean enabled
    ) {
    }

    /** Peripheral read shape. {@code id} is the ULID public id. */
    public record PeripheralResponse(
        String id,
        String registerId,
        String type,
        String connection,
        Map<String, Object> config,
        boolean enabled,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {
    }

    /** Register read shape. {@code id} is the ULID public id. */
    public record RegisterResponse(
        String id,
        String locationId,
        String code,
        String displayName,
        String operatingMode,
        String status,
        List<PeripheralResponse> peripherals,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {
    }

    /** FR-25.2 / FR-25.3 — Test Print / Test Open result. */
    public record PeripheralTestResult(
        boolean success,
        String detail
    ) {
    }
}

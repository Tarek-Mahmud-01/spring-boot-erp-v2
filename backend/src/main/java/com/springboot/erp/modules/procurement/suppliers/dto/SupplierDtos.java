package com.springboot.erp.modules.procurement.suppliers.dto;

import com.springboot.erp.modules.procurement.suppliers.domain.SupplierType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * Wire DTOs for ENT-026 Supplier + ENT-026a SupplierAttachment (records — ARCHITECTURE.md §2).
 * Mirrors the reference Pydantic constraints: name required, 3-letter currencies, non-negative
 * credit / opening balance, DEBIT|CREDIT side. {@code code} is server-generated (never on the
 * request). Cross-slice ids are ULID {@code char(26)} strings; {@code id} is the ULID public id.
 */
public final class SupplierDtos {

    private SupplierDtos() {
    }

    /** POST /api/procurement/suppliers body. */
    public record SupplierCreateRequest(
        @NotBlank @Size(max = 200) String name,
        SupplierType type,
        @Size(min = 26, max = 26) String locationId,
        Map<String, Object> contact,
        Map<String, Object> address,
        @Size(max = 100) String paymentTerms,
        @Size(max = 3) String defaultCurrency,
        @Size(max = 100) String taxRegistrationNo,
        @Size(max = 20) String abn,
        Map<String, Object> bankDetails,
        @PositiveOrZero long creditLimitAmount,
        @Size(max = 3) String creditLimitCurrency,
        @PositiveOrZero long openingBalanceAmount,
        @Size(min = 3, max = 3) String openingBalanceCurrency,
        @Pattern(regexp = "DEBIT|CREDIT") String openingBalanceSide,
        LocalDate openingBalanceDate,
        @Positive BigDecimal openingBalanceExchangeRate,
        @Size(min = 26, max = 26) String openingBalanceAccountId
    ) {
    }

    /** PATCH /api/procurement/suppliers/{id} body — every field optional; null = leave unchanged. */
    public record SupplierUpdateRequest(
        @Size(max = 200) String name,
        SupplierType type,
        @Size(min = 26, max = 26) String locationId,
        Map<String, Object> contact,
        Map<String, Object> address,
        @Size(max = 100) String paymentTerms,
        @Size(min = 3, max = 3) String defaultCurrency,
        @Size(max = 100) String taxRegistrationNo,
        @Size(max = 20) String abn,
        Map<String, Object> bankDetails,
        @PositiveOrZero Long creditLimitAmount,
        @Size(min = 3, max = 3) String creditLimitCurrency,
        @PositiveOrZero Long openingBalanceAmount,
        @Size(min = 3, max = 3) String openingBalanceCurrency,
        @Pattern(regexp = "DEBIT|CREDIT") String openingBalanceSide,
        LocalDate openingBalanceDate,
        @Positive BigDecimal openingBalanceExchangeRate,
        @Size(min = 26, max = 26) String openingBalanceAccountId,
        Long version
    ) {
    }

    /** PATCH /api/procurement/suppliers/{id}/status body. */
    public record SupplierStatusRequest(
        @NotBlank String status,
        @Size(max = 500) String blockReason
    ) {
    }

    /** Supplier read shape. {@code id} is the ULID public id; {@code status} the wire label. */
    public record SupplierResponse(
        String id,
        String code,
        String name,
        String type,
        String locationId,
        Map<String, Object> contact,
        Map<String, Object> address,
        String paymentTerms,
        String defaultCurrency,
        String taxRegistrationNo,
        String abn,
        Map<String, Object> bankDetails,
        long creditLimitAmount,
        String creditLimitCurrency,
        long openingBalanceAmount,
        String openingBalanceCurrency,
        String openingBalanceSide,
        LocalDate openingBalanceDate,
        BigDecimal openingBalanceExchangeRate,
        String openingBalanceAccountId,
        String status,
        String displayStatusTone,
        String blockReason,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {
    }

    /** SupplierAttachment read shape. */
    public record SupplierAttachmentResponse(
        String id,
        String fileName,
        long fileSize,
        String mimeType,
        String storageKey,
        String uploadedBy,
        Instant createdAt
    ) {
    }

    /**
     * Register-attachment metadata (POST). Binary upload + media storage is deferred; the client
     * supplies the storage key / file metadata produced by the (future) upload endpoint.
     */
    public record SupplierAttachmentRequest(
        @NotBlank @Size(max = 255) String fileName,
        @PositiveOrZero long fileSize,
        @NotBlank @Size(max = 100) String mimeType,
        @NotBlank @Size(max = 500) String storageKey
    ) {
    }
}

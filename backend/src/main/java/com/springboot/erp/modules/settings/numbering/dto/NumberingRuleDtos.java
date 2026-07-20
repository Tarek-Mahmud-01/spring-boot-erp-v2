package com.springboot.erp.modules.settings.numbering.dto;

import com.springboot.erp.modules.settings.numbering.domain.DocumentType;
import com.springboot.erp.modules.settings.numbering.domain.ResetCadence;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Request/response records for ENT-006 Numbering Rule. Field constraints mirror
 * the reference Pydantic schemas (padding 4..10, start_value &gt;= 1, prefix &lt;= 10,
 * company id a 26-char ULID).
 */
public final class NumberingRuleDtos {

    private NumberingRuleDtos() {
    }

    /** Body for {@code POST /api/numbering-rules}. */
    public record NumberingRuleCreateRequest(
        @NotBlank @Size(min = 26, max = 26) String companyId,
        @NotNull DocumentType documentType,
        @Size(max = 10) String prefix,
        @Min(4) @Max(10) int padding,
        ResetCadence resetCadence,
        @Min(1) long startValue
    ) {
        public NumberingRuleCreateRequest {
            if (prefix == null) {
                prefix = "";
            }
            if (resetCadence == null) {
                resetCadence = ResetCadence.NEVER;
            }
            if (startValue == 0) {
                startValue = 1;
            }
        }
    }

    /**
     * Body for {@code PATCH /api/numbering-rules/{publicId}}. All fields optional
     * (null = leave unchanged); {@code version} carries the optimistic-lock check.
     */
    public record NumberingRuleUpdateRequest(
        @Size(max = 10) String prefix,
        @Min(4) @Max(10) Integer padding,
        ResetCadence resetCadence,
        @Min(1) Long startValue,
        Long version
    ) {
    }

    /** Body for {@code POST /api/numbering-rules/{publicId}/allocate}. */
    public record NumberingAllocateRequest(
        @NotNull LocalDate documentDate
    ) {
    }

    /** Read model — exposes the public id as {@code id}, never the internal Long. */
    public record NumberingRuleResponse(
        String id,
        String companyId,
        DocumentType documentType,
        String prefix,
        int padding,
        ResetCadence resetCadence,
        long startValue,
        long currentValue,
        String currentWindowKey,
        long totalIssued,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {
    }

    /** Result of consuming one sequence number. */
    public record NumberingAllocateResponse(
        String number,
        long sequenceValue,
        String windowKey
    ) {
    }
}

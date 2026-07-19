package com.guru.erp.modules.finance.gl.mapper;

import com.guru.erp.modules.finance.gl.domain.GlPostingLog;
import com.guru.erp.modules.finance.gl.domain.JournalEntry;
import com.guru.erp.modules.finance.gl.domain.JournalLine;
import com.guru.erp.modules.finance.gl.domain.VoucherType;
import com.guru.erp.modules.finance.gl.dto.GlPostingLogDtos.GlPostingLogResponse;
import com.guru.erp.modules.finance.gl.dto.JournalEntryDtos.JournalEntryResponse;
import com.guru.erp.modules.finance.gl.dto.JournalLineDtos.JournalLineResponse;
import com.guru.erp.modules.finance.gl.dto.VoucherTypeDtos.VoucherTypeResponse;
import org.springframework.stereotype.Component;

/**
 * Hand-written entity -&gt; DTO mapping (no MapStruct annotation processor wired for this slice yet;
 * the shape is simple enough — enums to name(), publicId as {@code id} — that a small
 * {@code @Component} is clearer than adding a build-time dependency for four flat records).
 */
@Component
public class GlMapper {

    public JournalEntryResponse toResponse(JournalEntry e) {
        return new JournalEntryResponse(
            e.getPublicId(),
            e.getCompanyId(),
            e.getLocationId(),
            e.getVoucherType(),
            e.getVoucherNumber(),
            e.getEntryDate(),
            e.getPeriodCode(),
            e.getReference(),
            e.getNarration(),
            e.getStatus().name(),
            e.getLines().stream().map(this::toResponse).toList(),
            e.getTotalDebit(),
            e.getTotalCredit(),
            e.getTotalDebit() == e.getTotalCredit(),
            e.getReversedById(),
            e.getPostedAt(),
            e.getPostedBy(),
            e.getCreatedAt(),
            e.getCreatedBy(),
            e.getUpdatedAt(),
            e.getUpdatedBy(),
            e.getVersion());
    }

    public JournalLineResponse toResponse(JournalLine l) {
        return new JournalLineResponse(
            l.getPublicId(),
            l.getAccountId(),
            l.getHolderType().name(),
            l.getHolderId(),
            l.getNarration(),
            l.getDebit(),
            l.getCredit(),
            l.getCurrency(),
            l.getExchangeRate(),
            l.getBaseDebit(),
            l.getBaseCredit(),
            l.getLocationId());
    }

    public VoucherTypeResponse toResponse(VoucherType v) {
        return new VoucherTypeResponse(
            v.getPublicId(),
            v.getCode(),
            v.getPrefix(),
            v.getName(),
            v.getDescription(),
            v.isOperational(),
            v.getOperationalModule(),
            v.isActive(),
            v.getVersion());
    }

    public GlPostingLogResponse toResponse(GlPostingLog g) {
        return new GlPostingLogResponse(
            g.getPublicId(),
            g.getCompanyId(),
            g.getSourceKind(),
            g.getSourceRef(),
            g.getEventType(),
            g.getJournalEntryId(),
            g.getStatus().name(),
            g.getAttempts(),
            g.getLastError());
    }
}

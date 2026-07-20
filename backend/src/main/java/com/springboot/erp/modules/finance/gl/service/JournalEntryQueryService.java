package com.springboot.erp.modules.finance.gl.service;

import com.springboot.erp.modules.finance.gl.domain.JournalEntry;
import com.springboot.erp.modules.finance.gl.domain.JournalEntryStatus;
import com.springboot.erp.modules.finance.gl.dto.JournalEntryDtos.JournalEntryResponse;
import com.springboot.erp.modules.finance.gl.mapper.GlMapper;
import com.springboot.erp.modules.finance.gl.repository.JournalEntryRepository;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.web.PageResponse;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only queries over {@link JournalEntry} — reference {@code list_journal_entries} / {@code get_journal_entry}. */
@Service
@Transactional(readOnly = true)
public class JournalEntryQueryService {

    private final JournalEntryRepository repository;
    private final GlMapper mapper;

    public JournalEntryQueryService(JournalEntryRepository repository, GlMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public PageResponse<JournalEntryResponse> list(String companyId, String status, String voucherType,
                                                   LocalDate fromDate, LocalDate toDate, String search,
                                                   Pageable pageable) {
        JournalEntryStatus statusEnum = status != null
            ? JournalEntryStatus.valueOf(status.toUpperCase(java.util.Locale.ROOT)) : null;
        return PageResponse.of(
            repository.search(companyId, statusEnum, voucherType, fromDate, toDate, search, pageable),
            mapper::toResponse);
    }

    public JournalEntryResponse get(String publicId) {
        JournalEntry entry = repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("JournalEntry", publicId));
        return mapper.toResponse(entry);
    }
}

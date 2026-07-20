package com.springboot.erp.modules.finance.gl.controller;

import com.springboot.erp.modules.finance.gl.dto.JournalEntryDtos.JournalEntryCreateRequest;
import com.springboot.erp.modules.finance.gl.dto.JournalEntryDtos.JournalEntryResponse;
import com.springboot.erp.modules.finance.gl.dto.JournalEntryDtos.JournalEntryReverseRequest;
import com.springboot.erp.modules.finance.gl.dto.JournalEntryDtos.JournalEntryUpdateRequest;
import com.springboot.erp.modules.finance.gl.service.JournalEntryCommandService;
import com.springboot.erp.modules.finance.gl.service.JournalEntryQueryService;
import com.springboot.erp.modules.finance.gl.service.PostingService;
import com.springboot.erp.modules.finance.gl.service.ReversalService;
import com.springboot.erp.platform.web.PageResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Journal entry CRUD + workflow endpoints (reference {@code views/journal_entries.py}
 * {@code JournalEntryListView} / {@code JournalEntryDetailView} / {@code JournalEntryPostActionView} /
 * {@code JournalEntryReverseActionView}) — thin, delegates every rule to the GL services.
 */
@RestController
@RequestMapping("/api/finance/gl/journal-entries")
public class JournalEntryController {

    private final JournalEntryCommandService command;
    private final JournalEntryQueryService query;
    private final PostingService posting;
    private final ReversalService reversal;

    public JournalEntryController(JournalEntryCommandService command, JournalEntryQueryService query,
                                  PostingService posting, ReversalService reversal) {
        this.command = command;
        this.query = query;
        this.posting = posting;
        this.reversal = reversal;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('finance.journal.read')")
    public PageResponse<JournalEntryResponse> list(
            @RequestParam String companyId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String voucherType,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        return query.list(companyId, status, voucherType, fromDate, toDate, search, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('finance.journal.read')")
    public JournalEntryResponse get(@PathVariable String publicId) {
        return query.get(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('finance.journal.write')")
    public JournalEntryResponse create(@Valid @RequestBody JournalEntryCreateRequest request) {
        return command.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('finance.journal.write')")
    public JournalEntryResponse update(@PathVariable String publicId,
                                       @Valid @RequestBody JournalEntryUpdateRequest request) {
        return command.update(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('finance.journal.write')")
    public void delete(@PathVariable String publicId) {
        command.delete(publicId);
    }

    /** DRAFT -&gt; POSTED; rejects with VALIDATION_FAILED if the voucher does not balance exactly. */
    @PostMapping("/{publicId}/post")
    @PreAuthorize("hasAuthority('finance.journal.post')")
    public JournalEntryResponse post(@PathVariable String publicId) {
        return posting.post(publicId);
    }

    /** POSTED -&gt; REVERSED, creating a linked mirror-image entry. */
    @PostMapping("/{publicId}/reverse")
    @PreAuthorize("hasAuthority('finance.journal.post')")
    public JournalEntryResponse reverse(@PathVariable String publicId,
                                        @Valid @RequestBody JournalEntryReverseRequest request) {
        return reversal.reverse(publicId, request);
    }
}

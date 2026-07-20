package com.springboot.erp.modules.finance.gl.controller;

import com.springboot.erp.modules.finance.gl.dto.GlPostingLogDtos.GlPostingLogResponse;
import com.springboot.erp.modules.finance.gl.service.GlPostingLogQueryService;
import com.springboot.erp.platform.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only reconciliation feed over {@code GlPostingLog} — "did this POS event post to the GL?"
 * (reference: the GL exception feed backing {@code diagnose_pos_gl}). Write access to this table is
 * exclusively through {@link com.springboot.erp.modules.finance.gl.service.GlPostingConsumerService}'s
 * check-then-insert; there is deliberately no create/update endpoint here.
 */
@RestController
@RequestMapping("/api/finance/gl/posting-log")
public class GlPostingLogController {

    private final GlPostingLogQueryService query;

    public GlPostingLogController(GlPostingLogQueryService query) {
        this.query = query;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('finance.gl_posting_log.read')")
    public PageResponse<GlPostingLogResponse> list(@PageableDefault(size = 50) Pageable pageable) {
        return query.list(pageable);
    }

    @GetMapping("/by-source")
    @PreAuthorize("hasAuthority('finance.gl_posting_log.read')")
    public GlPostingLogResponse getBySource(@RequestParam String sourceKind, @RequestParam String sourceRef) {
        return query.getBySource(sourceKind, sourceRef);
    }
}

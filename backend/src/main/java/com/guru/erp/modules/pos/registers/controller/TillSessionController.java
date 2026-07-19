package com.guru.erp.modules.pos.registers.controller;

import com.guru.erp.modules.pos.registers.dto.TillDtos.TillCloseRequest;
import com.guru.erp.modules.pos.registers.dto.TillDtos.TillMovementRequest;
import com.guru.erp.modules.pos.registers.dto.TillDtos.TillOpenRequest;
import com.guru.erp.modules.pos.registers.dto.TillDtos.TillReportResponse;
import com.guru.erp.modules.pos.registers.dto.TillDtos.TillSessionResponse;
import com.guru.erp.modules.pos.registers.service.TillSessionCommandService;
import com.guru.erp.modules.pos.registers.service.TillSessionQueryService;
import com.guru.erp.platform.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * PosTillSession / PosTillMovement endpoints (US-037 FR-191..194) — thin
 * controller, {@code @PreAuthorize} per method. Business rules live in the
 * command/query services.
 */
@RestController
@RequestMapping("/api/pos/till")
public class TillSessionController {

    private final TillSessionCommandService command;
    private final TillSessionQueryService query;

    public TillSessionController(TillSessionCommandService command, TillSessionQueryService query) {
        this.command = command;
        this.query = query;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('pos.till.read')")
    public PageResponse<TillSessionResponse> list(@RequestParam(required = false) String registerId,
                                                  @RequestParam(required = false) String status,
                                                  @PageableDefault(size = 50) Pageable pageable) {
        return query.list(registerId, status, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('pos.till.read')")
    public TillSessionResponse get(@PathVariable String publicId) {
        return query.get(publicId);
    }

    /** The register's currently OPEN session, or {@code null} if none. */
    @GetMapping("/open")
    @PreAuthorize("hasAuthority('pos.till.read')")
    public TillSessionResponse getOpenForRegister(@RequestParam String registerId) {
        return query.getOpenForRegister(registerId);
    }

    @GetMapping("/{publicId}/report")
    @PreAuthorize("hasAuthority('pos.till.read')")
    public TillReportResponse report(@PathVariable String publicId,
                                     @RequestParam(defaultValue = "X") String reportType) {
        return query.report(publicId, reportType);
    }

    /** FR-191 — open a till with an opening float. One OPEN till per register. */
    @PostMapping("/open")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('pos.till.write')")
    public TillSessionResponse open(@Valid @RequestBody TillOpenRequest request) {
        return command.open(request);
    }

    /** FR-192 — record a manual cash pickup / drop / payout. */
    @PostMapping("/{publicId}/movements")
    @PreAuthorize("hasAuthority('pos.till.write')")
    public TillSessionResponse recordMovement(@PathVariable String publicId,
                                              @Valid @RequestBody TillMovementRequest request) {
        return command.recordMovement(publicId, request);
    }

    /** FR-193 — close a till. Manager credentials required only over the variance threshold. */
    @PostMapping("/{publicId}/close")
    @PreAuthorize("hasAuthority('pos.till.write')")
    public TillSessionResponse close(@PathVariable String publicId,
                                     @Valid @RequestBody TillCloseRequest request) {
        return command.close(publicId, request);
    }
}

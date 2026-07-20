package com.springboot.erp.modules.procurement.returns.controller;

import com.springboot.erp.modules.procurement.returns.dto.ReturnDtos.ReturnCreateRequest;
import com.springboot.erp.modules.procurement.returns.dto.ReturnDtos.ReturnResponse;
import com.springboot.erp.modules.procurement.returns.dto.ReturnDtos.ReturnTransitionRequest;
import com.springboot.erp.modules.procurement.returns.dto.ReturnDtos.ReturnUpdateRequest;
import com.springboot.erp.modules.procurement.returns.service.ReturnCommandService;
import com.springboot.erp.modules.procurement.returns.service.ReturnQueryService;
import com.springboot.erp.platform.web.PageResponse;
import jakarta.validation.Valid;
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
 * ENT-031 SupplierReturn endpoints — thin controller, {@code @PreAuthorize} per method. Business
 * rules live in the command/query services. The special {@code /transition} endpoint maps the
 * reference Draft → Confirmed sub-action. Email / PDF debit-note rendering from the reference is
 * deferred (out of scope for the port).
 */
@RestController
@RequestMapping("/api/procurement/returns")
public class ReturnController {

    private final ReturnCommandService command;
    private final ReturnQueryService query;

    public ReturnController(ReturnCommandService command, ReturnQueryService query) {
        this.command = command;
        this.query = query;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('procurement.return.read')")
    public PageResponse<ReturnResponse> list(
            @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) String grnId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        return query.list(supplierId, grnId, status, search, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('procurement.return.read')")
    public ReturnResponse get(@PathVariable String publicId) {
        return query.get(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('procurement.return.write')")
    public ReturnResponse create(@Valid @RequestBody ReturnCreateRequest request) {
        return command.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('procurement.return.write')")
    public ReturnResponse update(@PathVariable String publicId,
                                 @Valid @RequestBody ReturnUpdateRequest request) {
        return command.update(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('procurement.return.write')")
    public void delete(@PathVariable String publicId) {
        command.delete(publicId);
    }

    /** Reference transition_route — Draft → Confirmed, posting the debit note + relieving stock. */
    @PostMapping("/{publicId}/transition")
    @PreAuthorize("hasAuthority('procurement.return.write')")
    public ReturnResponse transition(@PathVariable String publicId,
                                     @Valid @RequestBody ReturnTransitionRequest request) {
        return command.transition(publicId, request);
    }
}

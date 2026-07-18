package com.guru.erp.modules.inventory.movements.controller;

import com.guru.erp.modules.inventory.movements.dto.TransferDtos.TransferCreateRequest;
import com.guru.erp.modules.inventory.movements.dto.TransferDtos.TransferReceiveRequest;
import com.guru.erp.modules.inventory.movements.dto.TransferDtos.TransferResponse;
import com.guru.erp.modules.inventory.movements.dto.TransferDtos.TransferUpdateRequest;
import com.guru.erp.modules.inventory.movements.service.TransferCommandService;
import com.guru.erp.modules.inventory.movements.service.TransferQueryService;
import com.guru.erp.modules.inventory.movements.service.TransferWorkflowService;
import com.guru.erp.platform.web.PageResponse;
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
 * ENT-042 StockTransfer endpoints — thin controller, {@code @PreAuthorize} per method. Business
 * rules live in the command/query services. Special endpoints (confirm / receive / complete) map
 * the reference transfer sub-actions.
 */
@RestController
@RequestMapping("/api/inventory/transfers")
public class TransferController {

    private final TransferCommandService command;
    private final TransferQueryService query;
    private final TransferWorkflowService workflow;

    public TransferController(TransferCommandService command, TransferQueryService query,
                              TransferWorkflowService workflow) {
        this.command = command;
        this.query = query;
        this.workflow = workflow;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory.transfer.read')")
    public PageResponse<TransferResponse> list(
            @RequestParam(required = false) String locationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        return query.list(locationId, status, search, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('inventory.transfer.read')")
    public TransferResponse get(@PathVariable String publicId) {
        return query.get(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('inventory.transfer.write')")
    public TransferResponse create(@Valid @RequestBody TransferCreateRequest request) {
        return command.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('inventory.transfer.write')")
    public TransferResponse update(@PathVariable String publicId,
                                   @Valid @RequestBody TransferUpdateRequest request) {
        return command.update(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('inventory.transfer.write')")
    public void delete(@PathVariable String publicId) {
        command.delete(publicId);
    }

    /** AC-023-1 — confirm the transfer (Draft → Approved), stocking out of the source. */
    @PostMapping("/{publicId}/confirm")
    @PreAuthorize("hasAuthority('inventory.transfer.write')")
    public TransferResponse confirm(@PathVariable String publicId) {
        return workflow.confirm(publicId);
    }

    /** AC-023-2 — receive the transfer into the destination (per-line quantities). */
    @PostMapping("/{publicId}/receive")
    @PreAuthorize("hasAuthority('inventory.transfer.write')")
    public TransferResponse receive(@PathVariable String publicId,
                                    @Valid @RequestBody TransferReceiveRequest request) {
        return workflow.receive(publicId, request);
    }

    /** One-shot Draft → Complete (confirm + clean receive). */
    @PostMapping("/{publicId}/complete")
    @PreAuthorize("hasAuthority('inventory.transfer.write')")
    public TransferResponse complete(@PathVariable String publicId) {
        return workflow.complete(publicId);
    }
}

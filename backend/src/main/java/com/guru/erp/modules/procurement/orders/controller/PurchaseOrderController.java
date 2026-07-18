package com.guru.erp.modules.procurement.orders.controller;

import com.guru.erp.modules.procurement.orders.dto.PoDtos.PoCreateRequest;
import com.guru.erp.modules.procurement.orders.dto.PoDtos.PoResponse;
import com.guru.erp.modules.procurement.orders.dto.PoDtos.PoTransitionRequest;
import com.guru.erp.modules.procurement.orders.dto.PoDtos.PoUpdateRequest;
import com.guru.erp.modules.procurement.orders.dto.PoDtos.PoVersionResponse;
import com.guru.erp.modules.procurement.orders.service.PurchaseOrderCommandService;
import com.guru.erp.modules.procurement.orders.service.PurchaseOrderQueryService;
import com.guru.erp.platform.web.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
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
 * ENT-028 PurchaseOrder endpoints — thin controller, {@code @PreAuthorize} per method. Business
 * rules live in the command/query services. Special endpoints (transition, versions) map the
 * reference PO sub-actions; workflow moves (submit / approve / send / receive / close / cancel) all
 * go through the one transition endpoint. Direct-PO auto-chaining and email/PDF are deferred.
 */
@RestController
@RequestMapping("/api/procurement/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderCommandService command;
    private final PurchaseOrderQueryService query;

    public PurchaseOrderController(PurchaseOrderCommandService command,
                                   PurchaseOrderQueryService query) {
        this.command = command;
        this.query = query;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('procurement.order.read')")
    public PageResponse<PoResponse> list(
            @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) String locationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isDirect,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        return query.list(supplierId, locationId, status, isDirect, search, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('procurement.order.read')")
    public PoResponse get(@PathVariable String publicId) {
        return query.get(publicId);
    }

    /** FR-092 — the PO's amendment version history, oldest first. */
    @GetMapping("/{publicId}/versions")
    @PreAuthorize("hasAuthority('procurement.order.read')")
    public List<PoVersionResponse> versions(@PathVariable String publicId) {
        return query.versions(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('procurement.order.write')")
    public PoResponse create(@Valid @RequestBody PoCreateRequest request) {
        return command.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('procurement.order.write')")
    public PoResponse update(@PathVariable String publicId,
                             @Valid @RequestBody PoUpdateRequest request) {
        return command.update(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('procurement.order.write')")
    public void delete(@PathVariable String publicId) {
        command.delete(publicId);
    }

    /**
     * FR-090 / FR-093 — workflow move: submit / approve / send / receive / partially-receive /
     * close / cancel. The target state is in the request body ({@code toStatus}).
     */
    @PostMapping("/{publicId}/transition")
    @PreAuthorize("hasAuthority('procurement.order.write')")
    public PoResponse transition(@PathVariable String publicId,
                                 @Valid @RequestBody PoTransitionRequest request) {
        return command.transition(publicId, request);
    }
}

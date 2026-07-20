package com.springboot.erp.modules.pos.transactions.controller;

import com.springboot.erp.modules.pos.transactions.dto.TransactionDtos.AddLineRequest;
import com.springboot.erp.modules.pos.transactions.dto.TransactionDtos.AgeVerifyRequest;
import com.springboot.erp.modules.pos.transactions.dto.TransactionDtos.AttachCustomerRequest;
import com.springboot.erp.modules.pos.transactions.dto.TransactionDtos.PosTransactionResponse;
import com.springboot.erp.modules.pos.transactions.dto.TransactionDtos.TenderRequest;
import com.springboot.erp.modules.pos.transactions.dto.TransactionDtos.TransactionOpenRequest;
import com.springboot.erp.modules.pos.transactions.dto.TransactionDtos.UpdateLineRequest;
import com.springboot.erp.modules.pos.transactions.dto.TransactionDtos.VoidTransactionRequest;
import com.springboot.erp.modules.pos.transactions.service.TenderService;
import com.springboot.erp.modules.pos.transactions.service.TransactionCommandService;
import com.springboot.erp.modules.pos.transactions.service.TransactionQueryService;
import com.springboot.erp.modules.pos.transactions.service.TransactionWorkflowService;
import com.springboot.erp.platform.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * Core POS sale endpoints (US-031..035 — reference {@code app.pos.views} transaction/line/tender
 * routes) — thin controller, {@code @PreAuthorize} per method. Business rules live in the
 * command/query/tender services. Register/product/payment-method lookups are never hard-called
 * from here (see the DTO javadocs); the caller supplies the already-resolved facts.
 */
@RestController
@RequestMapping("/api/pos/transactions")
public class TransactionController {

    private final TransactionCommandService command;
    private final TransactionQueryService query;
    private final TenderService tenders;
    private final TransactionWorkflowService workflow;

    public TransactionController(TransactionCommandService command, TransactionQueryService query,
                                 TenderService tenders, TransactionWorkflowService workflow) {
        this.command = command;
        this.query = query;
        this.tenders = tenders;
        this.workflow = workflow;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('pos.transaction.read')")
    public PageResponse<PosTransactionResponse> list(
            @RequestParam(required = false) String registerId,
            @RequestParam(required = false) String locationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        return query.list(registerId, locationId, status, search, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('pos.transaction.read')")
    public PosTransactionResponse get(@PathVariable String publicId) {
        return query.get(publicId);
    }

    /** US-031 — open a new cart on a register. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('pos.transaction.write')")
    public PosTransactionResponse open(@Valid @RequestBody TransactionOpenRequest request) {
        return command.open(request);
    }

    /** FR-066/FR-215 — attach (or, with a null id, detach) a customer on an open sale. */
    @PatchMapping("/{publicId}/customer")
    @PreAuthorize("hasAuthority('pos.transaction.write')")
    public PosTransactionResponse setCustomer(@PathVariable String publicId,
                                              @Valid @RequestBody AttachCustomerRequest request) {
        return command.setCustomer(publicId, request);
    }

    /** US-031 FR-164 / US-032 FR-171 — add a product line to the cart. */
    @PostMapping("/{publicId}/lines")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('pos.transaction.write')")
    public PosTransactionResponse addLine(@PathVariable String publicId,
                                          @Valid @RequestBody AddLineRequest request) {
        return command.addLine(publicId, request);
    }

    /** FR-164 — change a line's quantity; FR-167 rejects qty &lt;= 0. */
    @PatchMapping("/{publicId}/lines/{lineId}")
    @PreAuthorize("hasAuthority('pos.transaction.write')")
    public PosTransactionResponse setLineQty(@PathVariable String publicId, @PathVariable String lineId,
                                             @Valid @RequestBody UpdateLineRequest request) {
        return command.setLineQty(publicId, lineId, request);
    }

    /** FR-165 — void a line before payment; after payment requires a refund (a different sub-slice). */
    @PostMapping("/{publicId}/lines/{lineId}/void")
    @PreAuthorize("hasAuthority('pos.transaction.write')")
    public PosTransactionResponse voidLine(@PathVariable String publicId, @PathVariable String lineId) {
        return command.voidLine(publicId, lineId);
    }

    /** US-033 FR-172..175 — add a payment leg; cash may overpay (change due). */
    @PostMapping("/{publicId}/tenders")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('pos.transaction.write')")
    public PosTransactionResponse addTender(@PathVariable String publicId,
                                            @Valid @RequestBody TenderRequest request) {
        return tenders.addTender(publicId, request);
    }

    /** FR-176 — undo the most recent (non-reversed) tender before completing. */
    @PostMapping("/{publicId}/tenders/{tenderId}/undo")
    @PreAuthorize("hasAuthority('pos.transaction.write')")
    public PosTransactionResponse undoTender(@PathVariable String publicId, @PathVariable String tenderId) {
        return tenders.undoTender(publicId, tenderId);
    }

    /** FR-AU-011/014 — stamp that the cashier verified the customer's age for a restricted line. */
    @PostMapping("/{publicId}/age-verify")
    @PreAuthorize("hasAuthority('pos.transaction.write')")
    public PosTransactionResponse ageVerify(@PathVariable String publicId,
                                            @Valid @RequestBody AgeVerifyRequest request) {
        return workflow.ageVerify(publicId, request);
    }

    /** US-033 FR-173 — finalise a sale once the balance is zero; publishes {@code pos.sale.completed}. */
    @PostMapping("/{publicId}/complete")
    @PreAuthorize("hasAuthority('pos.transaction.write')")
    public PosTransactionResponse complete(@PathVariable String publicId) {
        return workflow.complete(publicId);
    }

    /** US-034 FR-181 — void a transaction (OPEN cart discarded; COMPLETED sale reversed). */
    @PostMapping("/{publicId}/void")
    @PreAuthorize("hasAuthority('pos.transaction.write')")
    public PosTransactionResponse voidTransaction(@PathVariable String publicId,
                                                  @Valid @RequestBody VoidTransactionRequest request) {
        return workflow.voidTransaction(publicId, request);
    }

    /** US-035 FR-182/183 — park an OPEN cart (non-empty, unpaid) so the lane is freed. */
    @PostMapping("/{publicId}/park")
    @PreAuthorize("hasAuthority('pos.transaction.write')")
    public PosTransactionResponse park(@PathVariable String publicId) {
        return workflow.park(publicId);
    }

    /** US-035 FR-184/185 — resume a parked cart back to OPEN. */
    @PostMapping("/{publicId}/resume")
    @PreAuthorize("hasAuthority('pos.transaction.write')")
    public PosTransactionResponse resume(@PathVariable String publicId) {
        return workflow.resume(publicId);
    }
}

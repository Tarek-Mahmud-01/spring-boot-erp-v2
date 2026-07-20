package com.springboot.erp.modules.procurement.suppliers.controller;

import com.springboot.erp.modules.procurement.suppliers.dto.SupplierDtos.SupplierAttachmentRequest;
import com.springboot.erp.modules.procurement.suppliers.dto.SupplierDtos.SupplierAttachmentResponse;
import com.springboot.erp.modules.procurement.suppliers.dto.SupplierDtos.SupplierCreateRequest;
import com.springboot.erp.modules.procurement.suppliers.dto.SupplierDtos.SupplierResponse;
import com.springboot.erp.modules.procurement.suppliers.dto.SupplierDtos.SupplierStatusRequest;
import com.springboot.erp.modules.procurement.suppliers.dto.SupplierDtos.SupplierUpdateRequest;
import com.springboot.erp.modules.procurement.suppliers.service.SupplierAttachmentService;
import com.springboot.erp.modules.procurement.suppliers.service.SupplierCommandService;
import com.springboot.erp.modules.procurement.suppliers.service.SupplierQueryService;
import com.springboot.erp.platform.web.PageResponse;
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
 * ENT-026 Supplier + ENT-026a SupplierAttachment endpoints — thin controller, {@code @PreAuthorize}
 * per method. Business rules live in the command / query / attachment services. Special endpoint:
 * status transition (reference {@code PATCH /suppliers/{id}/status}).
 */
@RestController
@RequestMapping("/api/procurement/suppliers")
public class SupplierController {

    private final SupplierCommandService command;
    private final SupplierQueryService query;
    private final SupplierAttachmentService attachments;

    public SupplierController(SupplierCommandService command, SupplierQueryService query,
                              SupplierAttachmentService attachments) {
        this.command = command;
        this.query = query;
        this.attachments = attachments;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('procurement.supplier.read')")
    public PageResponse<SupplierResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String locationId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        return query.list(status, type, locationId, search, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('procurement.supplier.read')")
    public SupplierResponse get(@PathVariable String publicId) {
        return query.get(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('procurement.supplier.write')")
    public SupplierResponse create(@Valid @RequestBody SupplierCreateRequest request) {
        return command.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('procurement.supplier.write')")
    public SupplierResponse update(@PathVariable String publicId,
                                   @Valid @RequestBody SupplierUpdateRequest request) {
        return command.update(publicId, request);
    }

    /** FR-078 — flip supplier status (Active / Inactive / Blocked); block requires a reason. */
    @PatchMapping("/{publicId}/status")
    @PreAuthorize("hasAuthority('procurement.supplier.write')")
    public SupplierResponse status(@PathVariable String publicId,
                                   @Valid @RequestBody SupplierStatusRequest request) {
        return command.setStatus(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('procurement.supplier.write')")
    public void delete(@PathVariable String publicId) {
        command.delete(publicId);
    }

    // --- attachments ---------------------------------------------------------

    @GetMapping("/{publicId}/attachments")
    @PreAuthorize("hasAuthority('procurement.attachment.read')")
    public List<SupplierAttachmentResponse> listAttachments(@PathVariable String publicId) {
        return attachments.list(publicId);
    }

    @PostMapping("/{publicId}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('procurement.attachment.write')")
    public SupplierAttachmentResponse addAttachment(@PathVariable String publicId,
                                                    @Valid @RequestBody SupplierAttachmentRequest request) {
        return attachments.add(publicId, request);
    }

    @DeleteMapping("/{publicId}/attachments/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('procurement.attachment.write')")
    public void deleteAttachment(@PathVariable String publicId, @PathVariable String attachmentId) {
        attachments.delete(publicId, attachmentId);
    }
}

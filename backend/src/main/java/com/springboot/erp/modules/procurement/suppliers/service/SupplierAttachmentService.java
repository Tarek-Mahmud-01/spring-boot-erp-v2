package com.springboot.erp.modules.procurement.suppliers.service;

import com.springboot.erp.modules.procurement.suppliers.domain.Supplier;
import com.springboot.erp.modules.procurement.suppliers.domain.SupplierAttachment;
import com.springboot.erp.modules.procurement.suppliers.dto.SupplierDtos.SupplierAttachmentRequest;
import com.springboot.erp.modules.procurement.suppliers.dto.SupplierDtos.SupplierAttachmentResponse;
import com.springboot.erp.modules.procurement.suppliers.mapper.SupplierMapper;
import com.springboot.erp.modules.procurement.suppliers.repository.SupplierAttachmentRepository;
import com.springboot.erp.modules.procurement.suppliers.repository.SupplierRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.security.CurrentUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ENT-026a SupplierAttachment use-cases (reference {@code SuppliersView} attachment routes):
 * register / list / delete supporting documents for a supplier.
 *
 * <p>Binary upload, size-limit enforcement, and media-URL rendering are deferred (see slice notes);
 * this service persists the attachment metadata (file name / size / mime / storage key) that a
 * future upload endpoint produces, and references the parent supplier by its ULID public id.
 */
@Service
public class SupplierAttachmentService {

    static final String AUDIT_ENTITY = "supplier_attachment";

    private final SupplierRepository supplierRepository;
    private final SupplierAttachmentRepository attachmentRepository;
    private final SupplierMapper mapper;
    private final AuditService auditService;
    private final CurrentUser currentUser;

    public SupplierAttachmentService(SupplierRepository supplierRepository,
                                     SupplierAttachmentRepository attachmentRepository,
                                     SupplierMapper mapper, AuditService auditService,
                                     CurrentUser currentUser) {
        this.supplierRepository = supplierRepository;
        this.attachmentRepository = attachmentRepository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    @Transactional
    public SupplierAttachmentResponse add(String supplierId, SupplierAttachmentRequest req) {
        Supplier s = loadSupplier(supplierId);
        SupplierAttachment att = new SupplierAttachment();
        att.setSupplierId(s.getPublicId());
        att.setFileName(req.fileName());
        att.setFileSize(req.fileSize());
        att.setMimeType(req.mimeType());
        att.setStorageKey(req.storageKey());
        att.setUploadedBy(currentUser.optional().map(p -> p.userPublicId()).orElse(null));

        SupplierAttachment saved = attachmentRepository.save(att);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SupplierAttachmentResponse> list(String supplierId) {
        Supplier s = loadSupplier(supplierId);
        return attachmentRepository.findBySupplierIdOrderByCreatedAtDesc(s.getPublicId()).stream()
            .map(mapper::toResponse)
            .toList();
    }

    @Transactional
    public void delete(String supplierId, String attachmentId) {
        Supplier s = loadSupplier(supplierId);
        SupplierAttachment att = attachmentRepository
            .findByPublicIdAndSupplierId(attachmentId, s.getPublicId())
            .orElseThrow(() -> DomainException.notFound("SupplierAttachment", attachmentId));
        SupplierAttachmentResponse before = mapper.toResponse(att);
        att.softDelete();
        attachmentRepository.save(att);
        auditService.record(AUDIT_ENTITY, attachmentId, AuditAction.DELETE, before, null);
    }

    private Supplier loadSupplier(String supplierId) {
        return supplierRepository.findByPublicId(supplierId)
            .orElseThrow(() -> DomainException.notFound("Supplier", supplierId));
    }
}

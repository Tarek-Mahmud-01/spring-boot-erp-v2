package com.springboot.erp.modules.finance.gl.service;

import com.springboot.erp.modules.finance.gl.domain.VoucherType;
import com.springboot.erp.modules.finance.gl.dto.VoucherTypeDtos.VoucherTypeCreateRequest;
import com.springboot.erp.modules.finance.gl.dto.VoucherTypeDtos.VoucherTypeResponse;
import com.springboot.erp.modules.finance.gl.dto.VoucherTypeDtos.VoucherTypeUpdateRequest;
import com.springboot.erp.modules.finance.gl.mapper.GlMapper;
import com.springboot.erp.modules.finance.gl.repository.JournalEntryRepository;
import com.springboot.erp.modules.finance.gl.repository.VoucherTypeRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD for the {@link VoucherType} catalogue (reference {@code VoucherTypeCreateRequest}/
 * {@code UpdateRequest} views). {@code operational} + {@code operationalModule} move together
 * (the {@code ck_voucher_types_operational_xor_module} check in V71) — a caller may not create an
 * operational type without naming the owning module, nor a non-operational one that names one.
 */
@Service
public class VoucherTypeService {

    private static final String AUDIT_ENTITY = "voucher_type";

    private final VoucherTypeRepository repository;
    private final JournalEntryRepository entryRepository;
    private final GlMapper mapper;
    private final AuditService auditService;

    public VoucherTypeService(VoucherTypeRepository repository, JournalEntryRepository entryRepository,
                              GlMapper mapper, AuditService auditService) {
        this.repository = repository;
        this.entryRepository = entryRepository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional
    public VoucherTypeResponse create(VoucherTypeCreateRequest req) {
        if (repository.existsByCode(req.code())) {
            throw new DomainException(ErrorCode.DUPLICATE, "Voucher type code already exists: " + req.code());
        }
        if (req.operational() && (req.operationalModule() == null || req.operationalModule().isBlank())) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "An operational voucher type must name its operationalModule");
        }
        if (!req.operational() && req.operationalModule() != null && !req.operationalModule().isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "A non-operational voucher type must not carry an operationalModule");
        }

        VoucherType vt = new VoucherType();
        vt.setCode(req.code());
        vt.setPrefix(req.prefix());
        vt.setName(req.name());
        vt.setDescription(req.description() != null ? req.description() : "");
        vt.setOperational(req.operational());
        vt.setOperationalModule(req.operational() ? req.operationalModule() : null);

        VoucherType saved = repository.save(vt);
        VoucherTypeResponse response = mapper.toResponse(saved);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null, response);
        return response;
    }

    @Transactional
    public VoucherTypeResponse update(String publicId, VoucherTypeUpdateRequest req) {
        VoucherType vt = load(publicId);
        if (vt.getVersion() != req.version()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK,
                "Voucher type " + publicId + " was modified concurrently");
        }
        VoucherTypeResponse before = mapper.toResponse(vt);

        if (req.name() != null) {
            vt.setName(req.name());
        }
        if (req.description() != null) {
            vt.setDescription(req.description());
        }
        if (req.prefix() != null) {
            vt.setPrefix(req.prefix());
        }
        if (req.active() != null) {
            vt.setActive(req.active());
        }

        VoucherType saved = repository.save(vt);
        VoucherTypeResponse after = mapper.toResponse(saved);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before, after);
        return after;
    }

    @Transactional
    public void delete(String publicId) {
        VoucherType vt = load(publicId);
        if (vt.isOperational()) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Operational voucher type " + publicId + " is owned by its module and cannot be deleted");
        }
        if (entryRepository.existsByVoucherType(vt.getCode())) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Voucher type " + publicId + " is referenced by existing journal entries and cannot be deleted");
        }
        VoucherTypeResponse before = mapper.toResponse(vt);
        vt.softDelete();
        repository.save(vt);
        auditService.record(AUDIT_ENTITY, vt.getPublicId(), AuditAction.DELETE, before, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<VoucherTypeResponse> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public VoucherTypeResponse get(String publicId) {
        return mapper.toResponse(load(publicId));
    }

    private VoucherType load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("VoucherType", publicId));
    }
}

package com.springboot.erp.modules.inventory.movements.service;

import com.springboot.erp.modules.inventory.movements.domain.StockTransfer;
import com.springboot.erp.modules.inventory.movements.domain.StockTransferLine;
import com.springboot.erp.modules.inventory.movements.domain.TransferStatus;
import com.springboot.erp.modules.inventory.movements.dto.TransferDtos.TransferCreateRequest;
import com.springboot.erp.modules.inventory.movements.dto.TransferDtos.TransferLineRequest;
import com.springboot.erp.modules.inventory.movements.dto.TransferDtos.TransferResponse;
import com.springboot.erp.modules.inventory.movements.dto.TransferDtos.TransferUpdateRequest;
import com.springboot.erp.modules.inventory.movements.mapper.MovementMapper;
import com.springboot.erp.modules.inventory.movements.repository.StockTransferRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.id.Ulid;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side CRUD use-cases for ENT-042 StockTransfer (US-023 / FR-124–128): create / update /
 * delete on the header+lines aggregate, plus the reference auto-complete shortcut (delegated to
 * {@link TransferWorkflowService}). Confirm / receive / complete live in the workflow service. One
 * audit row per mutation.
 */
@Service
public class TransferCommandService {

    static final String AUDIT_ENTITY = "stock_transfer";

    private final StockTransferRepository repository;
    private final MovementMapper mapper;
    private final AuditService auditService;
    private final TransferWorkflowService workflow;
    private final Clock clock = Clock.systemUTC();

    public TransferCommandService(StockTransferRepository repository, MovementMapper mapper,
                                  AuditService auditService, TransferWorkflowService workflow) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.workflow = workflow;
    }

    @Transactional
    public TransferResponse create(TransferCreateRequest req) {
        validateHeader(req.sourceLocationId(), req.destinationLocationId(), req.lines()
            .stream().map(TransferLineRequest::productId).toList());
        StockTransfer t = new StockTransfer();
        t.setNumber(generateNumber());
        t.setSourceLocationId(req.sourceLocationId());
        t.setDestinationLocationId(req.destinationLocationId());
        t.setNotes(req.notes());
        t.setTransferDate(resolveTransferDate(req.transferDate()));
        t.setStatus(TransferStatus.DRAFT);
        applyLines(t, req.lines());

        StockTransfer saved = repository.save(t);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null,
            mapper.toResponse(saved));
        if (req.autoComplete()) {
            workflow.confirmInternal(saved);
            workflow.receiveCleanInternal(saved);
        }
        return mapper.toResponse(saved);
    }

    @Transactional
    public TransferResponse update(String publicId, TransferUpdateRequest req) {
        StockTransfer t = load(publicId);
        checkVersion(t, req.version());
        if (t.getStatus() != TransferStatus.DRAFT) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Only a Draft transfer can be edited; this one is " + t.getStatus().wire());
        }
        validateHeader(req.sourceLocationId(), req.destinationLocationId(), req.lines()
            .stream().map(TransferLineRequest::productId).toList());
        TransferResponse before = mapper.toResponse(t);

        t.setSourceLocationId(req.sourceLocationId());
        t.setDestinationLocationId(req.destinationLocationId());
        t.setNotes(req.notes());
        if (req.transferDate() != null) {
            t.setTransferDate(resolveTransferDate(req.transferDate()));
        }
        t.clearLines();
        applyLines(t, req.lines());

        StockTransfer saved = repository.save(t);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(String publicId) {
        StockTransfer t = load(publicId);
        if (t.getStatus() == TransferStatus.COMPLETE) {
            throw new DomainException(ErrorCode.CONFLICT, "A completed transfer cannot be deleted");
        }
        TransferResponse before = mapper.toResponse(t);
        t.softDelete();
        repository.save(t);
        auditService.record(AUDIT_ENTITY, publicId, AuditAction.DELETE, before, null);
    }

    // --- helpers -------------------------------------------------------------

    private void applyLines(StockTransfer t, List<TransferLineRequest> lines) {
        int lineNo = 1;
        for (TransferLineRequest in : lines) {
            if (in.qtySent() == null || in.qtySent().signum() <= 0) {
                throw new DomainException(ErrorCode.VALIDATION_FAILED, "qty_sent must be positive");
            }
            StockTransferLine line = new StockTransferLine();
            line.setLineNo(lineNo++);
            line.setProductId(in.productId());
            line.setUomId(in.uomId());
            line.setVariantId(in.variantId());
            line.setQtySent(in.qtySent());
            t.addLine(line);
        }
    }

    private void validateHeader(String source, String destination, List<String> productIds) {
        // ck_transfers_different_locations + reference _no_duplicate_products.
        if (source.equals(destination)) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Source and destination location must be different");
        }
        Set<String> seen = new HashSet<>();
        for (String pid : productIds) {
            if (!seen.add(pid)) {
                throw new DomainException(ErrorCode.VALIDATION_FAILED,
                    "Each product can appear only once in a transfer: " + pid);
            }
        }
    }

    private LocalDate resolveTransferDate(LocalDate date) {
        // Reference _not_future — inventory moves on the day it moves, never the future.
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        if (date == null) {
            return today;
        }
        if (date.isAfter(today)) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "transfer_date cannot be in the future", Map.of("transferDate", date.toString()));
        }
        return date;
    }

    private String generateNumber() {
        // Self-contained document number — v2's numbering slice has no TRANSFER doc type yet
        // (documented as a TODO in the returned notes).
        String number;
        do {
            number = "TRF-" + Ulid.next().substring(16);
        } while (repository.existsByNumber(number));
        return number;
    }

    private StockTransfer load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("StockTransfer", publicId));
    }

    private void checkVersion(StockTransfer t, Long requestVersion) {
        if (requestVersion != null && requestVersion != t.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK,
                ErrorCode.OPTIMISTIC_LOCK.defaultDetail());
        }
    }
}

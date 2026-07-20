package com.springboot.erp.modules.inventory.stock.service;

import com.springboot.erp.modules.inventory.stock.domain.StockLedger;
import com.springboot.erp.modules.inventory.stock.domain.StockStatus;
import com.springboot.erp.modules.inventory.stock.dto.StockLedgerDtos.LedgerEntryResponse;
import com.springboot.erp.modules.inventory.stock.dto.StockLedgerDtos.LedgerPostRequest;
import com.springboot.erp.modules.inventory.stock.mapper.StockMapper;
import com.springboot.erp.modules.inventory.stock.repository.StockLedgerRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.money.Money;
import com.springboot.erp.platform.outbox.OutboxPublisher;
import com.springboot.erp.platform.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Append-only writer for the {@link StockLedger} (reference FR-114 / FR-118).
 * The ledger is never mutated — the only write is posting a new movement row.
 * Every post records an audit row and publishes a {@code stock.movement.posted}
 * outbox event so downstream slices (GL / valuation) react without a hard
 * cross-slice compile dependency.
 *
 * <p>Business invariants ported from the reference:
 * <ul>
 *   <li>REVALUATION rows must carry {@code qtySigned = 0} and a non-zero
 *       {@code valueDeltaAmount}; all other movements must have a non-zero
 *       {@code qtySigned} and a zero value delta.</li>
 *   <li>{@code occurredAt} defaults to now (UTC) when omitted.</li>
 * </ul>
 */
@Service
public class StockPostingService {

    private static final String AUDIT_ENTITY = "stock_ledger";
    private static final String EVENT_MOVEMENT_POSTED = "stock.movement.posted";
    private static final String DEFAULT_CURRENCY = "USD";

    private final StockLedgerRepository repository;
    private final StockMapper mapper;
    private final AuditService auditService;
    private final OutboxPublisher outboxPublisher;
    private final CurrentUser currentUser;

    public StockPostingService(StockLedgerRepository repository, StockMapper mapper,
                               AuditService auditService, OutboxPublisher outboxPublisher,
                               CurrentUser currentUser) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.outboxPublisher = outboxPublisher;
        this.currentUser = currentUser;
    }

    @Transactional
    public LedgerEntryResponse post(LedgerPostRequest req) {
        BigDecimal qty = req.qtySigned();
        boolean isRevaluation =
            req.movementType() == com.springboot.erp.modules.inventory.stock.domain.MovementType.REVALUATION;
        if (isRevaluation) {
            if (qty.signum() != 0) {
                throw new DomainException(ErrorCode.VALIDATION_FAILED,
                    "REVALUATION movements must carry qtySigned = 0 (value-only).");
            }
            if (req.valueDeltaAmount() == 0) {
                throw new DomainException(ErrorCode.VALIDATION_FAILED,
                    "REVALUATION movements must carry a non-zero valueDeltaAmount.");
            }
        } else {
            if (qty.signum() == 0) {
                throw new DomainException(ErrorCode.VALIDATION_FAILED,
                    "A quantity movement must carry a non-zero qtySigned.");
            }
            if (req.valueDeltaAmount() != 0) {
                throw new DomainException(ErrorCode.VALIDATION_FAILED,
                    "Only REVALUATION movements may carry a valueDeltaAmount.");
            }
        }

        StockLedger row = new StockLedger();
        row.setOccurredAt(req.occurredAt() != null ? req.occurredAt() : Instant.now());
        row.setProductId(req.productId());
        row.setVariantId(req.variantId());
        row.setLocationId(req.locationId());
        row.setStatus(req.status() != null ? req.status() : StockStatus.AVAILABLE);
        row.setMovementType(req.movementType());
        row.setQtySigned(qty);
        String currency = req.unitCostCurrency() != null
            ? req.unitCostCurrency().toUpperCase(java.util.Locale.ROOT) : DEFAULT_CURRENCY;
        row.setUnitCost(Money.ofMinor(req.unitCostAmount(), currency));
        row.setValueDeltaAmount(req.valueDeltaAmount());
        row.setSourceDocType(req.sourceDocType());
        row.setSourceDocId(req.sourceDocId());
        row.setBatchId(req.batchId());
        row.setExpiryDate(req.expiryDate());
        row.setActorUserId(currentUser.optional().map(p -> p.userPublicId()).orElse(null));
        row.setNotes(req.notes());

        StockLedger saved = repository.save(row);
        LedgerEntryResponse response = mapper.toResponse(saved);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null, response);
        outboxPublisher.publish(AUDIT_ENTITY, saved.getPublicId(), EVENT_MOVEMENT_POSTED, response);
        return response;
    }
}

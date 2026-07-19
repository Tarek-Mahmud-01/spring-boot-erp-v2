package com.guru.erp.modules.pos.auxiliary.service;

import com.guru.erp.modules.pos.auxiliary.domain.PosParkedSale;
import com.guru.erp.modules.pos.auxiliary.dto.ParkedSaleDtos.ParkSaleRequest;
import com.guru.erp.modules.pos.auxiliary.dto.ParkedSaleDtos.ParkedSaleResponse;
import com.guru.erp.modules.pos.auxiliary.repository.PosParkedSaleRepository;
import com.guru.erp.platform.audit.AuditAction;
import com.guru.erp.platform.audit.AuditService;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import com.guru.erp.platform.id.Ulid;
import com.guru.erp.platform.outbox.OutboxPublisher;
import com.guru.erp.platform.security.CurrentUser;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write + read use-cases for PosParkedSale (US-035 FR-182..186) — port of the reference
 * {@code park_transaction} / {@code resume_parked_sale} / {@code list_parked_sales} views.
 *
 * <p>The (not-yet-ported) PosTransaction aggregate lives in another sub-slice, so this service
 * never reads/writes it directly (vertical-slice rule): the caller (the transactions slice's
 * command service, once built, or — meanwhile — this slice's own controller) supplies the OPEN
 * cart's snapshot (line count / total / currency) up front, and is responsible for flipping the
 * transaction's own status (OPEN &lt;-&gt; PARKED) around these calls. This service owns only the
 * park-code lifecycle, retention, audit trail, and the {@code pos.sale.parked} outbox event.
 */
@Service
public class ParkedSaleService {

    static final String AUDIT_ENTITY = "pos_parked_sale";
    static final String EVENT_SALE_PARKED = "pos.sale.parked";
    static final String AGGREGATE = "pos_parked_sale";

    /** FR-183 default parked-sale retention. */
    static final long DEFAULT_RETENTION_HOURS = 24;

    private final PosParkedSaleRepository repository;
    private final AuditService auditService;
    private final OutboxPublisher outbox;
    private final CurrentUser currentUser;
    private final Clock clock = Clock.systemUTC();

    public ParkedSaleService(PosParkedSaleRepository repository, AuditService auditService,
                             OutboxPublisher outbox, CurrentUser currentUser) {
        this.repository = repository;
        this.auditService = auditService;
        this.outbox = outbox;
        this.currentUser = currentUser;
    }

    /**
     * US-035 FR-182/183 — park an OPEN cart under a short code so the lane is freed for the next
     * customer. The caller has already verified the transaction is OPEN, non-empty, and unpaid
     * (reference {@code _assert_open} / {@code PosCartEmptyError} / {@code PosTransactionAlreadyPaidError}).
     */
    @Transactional
    public ParkedSaleResponse park(String transactionId, String registerId, String locationId,
                                   int lineCount, long totalAmount, String currency,
                                   ParkSaleRequest request) {
        Instant now = Instant.now(clock);
        PosParkedSale parked = new PosParkedSale();
        parked.setTransactionId(transactionId);
        parked.setRegisterId(registerId);
        parked.setLocationId(locationId);
        parked.setParkCode(uniqueParkCode());
        parked.setParkedBy(currentUser.optional().map(p -> p.userPublicId()).orElse(null));
        parked.setParkedAt(now);
        parked.setExpiresAt(now.plus(Duration.ofHours(DEFAULT_RETENTION_HOURS)));
        parked.setNote(request == null ? null : request.note());

        PosParkedSale saved = repository.save(parked);

        Map<String, Object> after = Map.of(
            "parkCode", saved.getParkCode(),
            "transactionId", transactionId,
            "registerId", registerId,
            "locationId", locationId,
            "note", saved.getNote() == null ? "" : saved.getNote());
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null, after);
        outbox.publish(AGGREGATE, saved.getPublicId(), EVENT_SALE_PARKED, after);

        return toResponse(saved, lineCount, totalAmount, currency);
    }

    /**
     * US-035 FR-184/185 — resume a parked cart by code. Expired parks (past the retention window)
     * are rejected so a stale cart can't be reopened. Returns the resumed park row; the caller
     * flips the referenced transaction's status back to OPEN.
     */
    @Transactional
    public PosParkedSale resume(String parkCode) {
        PosParkedSale parked = repository.findByParkCodeAndResumedAtIsNull(parkCode.toUpperCase())
            .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND,
                "No active parked sale under code '%s'".formatted(parkCode),
                Map.of("parkCode", parkCode)));
        if (parked.isExpired(Instant.now(clock))) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Parked sale '%s' has expired".formatted(parkCode),
                Map.of("parkCode", parkCode));
        }
        parked.setResumedAt(Instant.now(clock));
        PosParkedSale saved = repository.save(parked);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE,
            Map.of("resumed", false),
            Map.of("resumed", true, "transactionId", saved.getTransactionId()));
        return saved;
    }

    /** US-035 FR-184 — list active (not yet resumed) parked carts, optionally by location. */
    @Transactional(readOnly = true)
    public Page<PosParkedSale> listActive(String locationId, Pageable pageable) {
        String loc = locationId == null || locationId.isBlank() ? null : locationId.trim();
        return repository.findActive(loc, pageable);
    }

    @Transactional(readOnly = true)
    public PosParkedSale get(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("PosParkedSale", publicId));
    }

    public ParkedSaleResponse toResponse(PosParkedSale parked, int lineCount, long totalAmount,
                                         String currency) {
        return new ParkedSaleResponse(
            parked.getPublicId(),
            parked.getParkCode(),
            parked.getTransactionId(),
            parked.getRegisterId(),
            parked.getLocationId(),
            parked.getNote(),
            lineCount,
            totalAmount,
            currency,
            parked.getParkedAt(),
            parked.getExpiresAt(),
            !parked.isActive());
    }

    /** Short, human-typeable park code, unique among *active* parks (reference {@code _unique_park_code}). */
    private String uniqueParkCode() {
        for (int i = 0; i < 10; i++) {
            String code = Ulid.next().substring(20).toUpperCase();
            if (!repository.existsByParkCodeAndResumedAtIsNull(code)) {
                return code;
            }
        }
        throw new IllegalStateException("could not allocate a unique park code");
    }
}

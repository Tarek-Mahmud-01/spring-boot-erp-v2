package com.guru.erp.modules.pos.auxiliary.service;

import com.guru.erp.modules.pos.auxiliary.domain.PosEvent;
import com.guru.erp.modules.pos.auxiliary.domain.PosEventType;
import com.guru.erp.modules.pos.auxiliary.dto.PosEventDtos.PosEventResponse;
import com.guru.erp.modules.pos.auxiliary.dto.PosEventDtos.RecordEventRequest;
import com.guru.erp.modules.pos.auxiliary.mapper.PosAuxMapper;
import com.guru.erp.modules.pos.auxiliary.repository.PosEventRepository;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import com.guru.erp.platform.security.CurrentUser;
import com.guru.erp.platform.web.PageResponse;
import java.time.Clock;
import java.time.Instant;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read + write use-cases for PosEvent (FR-AU-013 / FR-198 / FR-25.8) — port of the reference
 * {@code _record_pos_event} / {@code record_peripheral_failure} / {@code record_age_refusal} /
 * {@code list_peripheral_failures} views. Append-only: rows are created once and only ever
 * updated to mark them reviewed.
 */
@Service
public class PosEventService {

    private final PosEventRepository repository;
    private final PosAuxMapper mapper;
    private final CurrentUser currentUser;
    private final Clock clock = Clock.systemUTC();

    public PosEventService(PosEventRepository repository, PosAuxMapper mapper, CurrentUser currentUser) {
        this.repository = repository;
        this.mapper = mapper;
        this.currentUser = currentUser;
    }

    /** Append one POS-domain event row. */
    @Transactional
    public PosEventResponse record(RecordEventRequest request) {
        PosEventType type = parseType(request.type());
        PosEvent event = new PosEvent();
        event.setType(type);
        event.setRegisterId(request.registerId());
        event.setTransactionId(request.transactionId());
        event.setPayload(request.payload());
        event.setNeedsReview(request.needsReview());
        return mapper.toResponse(repository.save(event));
    }

    /** Same as {@link #record} but for internal callers that already hold a typed event type. */
    @Transactional
    public PosEvent recordInternal(PosEventType type, String registerId, String transactionId,
                                   java.util.Map<String, Object> payload, boolean needsReview) {
        PosEvent event = new PosEvent();
        event.setType(type);
        event.setRegisterId(registerId);
        event.setTransactionId(transactionId);
        event.setPayload(payload);
        event.setNeedsReview(needsReview);
        return repository.save(event);
    }

    /** Mark an event reviewed by the current user (or a nominated reviewer id). */
    @Transactional
    public PosEventResponse review(String publicId, String reviewedBy) {
        PosEvent event = load(publicId);
        String reviewer = reviewedBy != null && !reviewedBy.isBlank()
            ? reviewedBy
            : currentUser.optional().map(p -> p.userPublicId()).orElse(null);
        event.setReviewedBy(reviewer);
        event.setReviewedAt(Instant.now(clock));
        event.setNeedsReview(false);
        return mapper.toResponse(repository.save(event));
    }

    @Transactional(readOnly = true)
    public PosEventResponse get(String publicId) {
        return mapper.toResponse(load(publicId));
    }

    /** Filtered timeline — optional type / register / transaction / needs-review scoping. */
    @Transactional(readOnly = true)
    public PageResponse<PosEventResponse> list(String type, String registerId, String transactionId,
                                               Boolean needsReview, Pageable pageable) {
        PosEventType parsedType = type == null || type.isBlank() ? null : parseType(type);
        String reg = blankToNull(registerId);
        String txn = blankToNull(transactionId);
        return PageResponse.of(
            repository.search(parsedType, reg, txn, needsReview, pageable), mapper::toResponse);
    }

    private PosEvent load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("PosEvent", publicId));
    }

    private static PosEventType parseType(String raw) {
        try {
            return PosEventType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "Unknown POS event type: " + raw);
        }
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}

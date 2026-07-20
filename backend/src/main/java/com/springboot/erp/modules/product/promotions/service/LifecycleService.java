package com.springboot.erp.modules.product.promotions.service;

import com.springboot.erp.modules.product.promotions.domain.LifecycleState;
import com.springboot.erp.modules.product.promotions.domain.LifecycleTransition;
import com.springboot.erp.modules.product.promotions.dto.LifecycleDtos.LifecycleTransitionRequest;
import com.springboot.erp.modules.product.promotions.dto.LifecycleDtos.LifecycleTransitionResponse;
import com.springboot.erp.modules.product.promotions.mapper.LifecycleTransitionMapper;
import com.springboot.erp.modules.product.promotions.repository.LifecycleTransitionRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.security.CurrentUser;
import com.springboot.erp.platform.web.PageResponse;
import java.time.Instant;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-015 / FR-070..074 — product lifecycle governance. Records every state move
 * into the append-only {@link LifecycleTransition} ledger after validating it
 * against the FR-070 state machine ({@link LifecycleState#MACHINE}).
 *
 * <p>The Product row (with its current {@code lifecycle_state}) lives on the
 * catalog slice, so the caller supplies the {@code fromState}. When this
 * product already has ledger history, the supplied {@code fromState} is
 * cross-checked against the last recorded {@code toState} so a stale/spoofed
 * origin state is rejected (app-layer resolution — no hard cross-slice FK).
 */
@Service
public class LifecycleService {

    private static final String ENTITY = "product";

    private final LifecycleTransitionRepository repository;
    private final LifecycleTransitionMapper mapper;
    private final AuditService auditService;
    private final CurrentUser currentUser;

    public LifecycleService(LifecycleTransitionRepository repository,
                            LifecycleTransitionMapper mapper, AuditService auditService,
                            CurrentUser currentUser) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public PageResponse<LifecycleTransitionResponse> list(String productId, Pageable pageable) {
        return PageResponse.of(
            repository.findByProductIdOrderByChangedAtDescIdDesc(productId, pageable),
            mapper::toResponse);
    }

    /**
     * FR-070 / FR-074 — validate and record a lifecycle transition. Rejects a
     * move the state machine forbids, and a {@code fromState} that contradicts
     * the last recorded state for this product.
     */
    @Transactional
    public LifecycleTransitionResponse transition(LifecycleTransitionRequest req) {
        LifecycleState from = req.fromState();
        LifecycleState to = req.toState();

        repository.findFirstByProductIdOrderByChangedAtDescIdDesc(req.productId())
            .ifPresent(last -> {
                if (last.getToState() != from) {
                    throw new DomainException(ErrorCode.CONFLICT,
                        "Supplied fromState does not match the product's last recorded state",
                        Map.of("expected", last.getToState().name(), "supplied", from.name()));
                }
            });

        // Throws ILLEGAL_STATE_TRANSITION when the move is not allowed (FR-070).
        LifecycleState.MACHINE.transition(from, to);

        LifecycleTransition t = new LifecycleTransition();
        t.setProductId(req.productId());
        t.setFromState(from);
        t.setToState(to);
        t.setReason(req.reason());
        t.setChangedAt(Instant.now());
        t.setChangedBy(currentUser.optional().map(p -> p.userPublicId()).orElse(null));

        LifecycleTransition saved = repository.save(t);
        auditService.record(ENTITY, req.productId(), AuditAction.UPDATE,
            Map.of("lifecycle_state", from.name()),
            Map.of("lifecycle_state", to.name(), "reason", req.reason() == null ? "" : req.reason()));
        return mapper.toResponse(saved);
    }
}

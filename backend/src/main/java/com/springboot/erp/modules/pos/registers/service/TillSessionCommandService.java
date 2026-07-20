package com.springboot.erp.modules.pos.registers.service;

import com.springboot.erp.modules.pos.registers.domain.PosTillMovement;
import com.springboot.erp.modules.pos.registers.domain.PosTillSession;
import com.springboot.erp.modules.pos.registers.domain.Register;
import com.springboot.erp.modules.pos.registers.domain.RegisterOperatingMode;
import com.springboot.erp.modules.pos.registers.domain.RegisterStatus;
import com.springboot.erp.modules.pos.registers.domain.TillMovementType;
import com.springboot.erp.modules.pos.registers.domain.TillSessionStatus;
import com.springboot.erp.modules.pos.registers.dto.TillDtos.TillCloseRequest;
import com.springboot.erp.modules.pos.registers.dto.TillDtos.TillMovementRequest;
import com.springboot.erp.modules.pos.registers.dto.TillDtos.TillOpenRequest;
import com.springboot.erp.modules.pos.registers.dto.TillDtos.TillSessionResponse;
import com.springboot.erp.modules.pos.registers.mapper.RegistersMapper;
import com.springboot.erp.modules.pos.registers.repository.PosTillSessionRepository;
import com.springboot.erp.modules.pos.registers.repository.RegisterRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.money.Money;
import com.springboot.erp.platform.outbox.OutboxPublisher;
import com.springboot.erp.platform.security.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side use-cases for PosTillSession / PosTillMovement (US-037 /
 * FR-191..194). Ports the reference open / record-cash-movement / close flow:
 * a preflight (register must be ACTIVE, and MANAGER_ONLY registers require the
 * operator to hold {@code pos.manager.operate} — reference
 * {@code assert_register_can_open_session}), the "one OPEN session per
 * register" invariant, a manual-discount-style manager step-up when the
 * counted-cash variance at close exceeds the tenant threshold, and one audit
 * row + outbox event per state change.
 */
@Service
public class TillSessionCommandService {

    static final String AUDIT_ENTITY = "pos_till_session";
    static final String EVENT_TILL_OPENED = "pos.till.opened";
    static final String EVENT_TILL_CLOSED = "pos.till.closed";
    static final String DEFAULT_CURRENCY = "USD";
    static final String PERMISSION_MANAGER_OPERATE = "pos.manager.operate";
    static final String PERMISSION_TILL_MANAGE = "pos.till.manage";

    /** FR-193 — cash-count variance above this (minor units) needs manager sign-off before close. */
    static final long CASH_VARIANCE_THRESHOLD_MINOR = 500;

    /** FR-192 — PICKUP/SALE add cash to the drawer; DROP/CHANGE/REFUND_OUT remove it. */
    private static final Set<TillMovementType> CASH_IN = EnumSet.of(TillMovementType.SALE, TillMovementType.PICKUP);
    private static final Set<TillMovementType> CASH_OUT =
        EnumSet.of(TillMovementType.DROP, TillMovementType.CHANGE, TillMovementType.REFUND_OUT);
    /** Movement types postable through the manual cash-movement endpoint (SALE/CHANGE are system-only). */
    private static final Set<TillMovementType> MANUAL_MOVEMENT_TYPES =
        EnumSet.of(TillMovementType.PICKUP, TillMovementType.DROP, TillMovementType.REFUND_OUT);

    private final PosTillSessionRepository sessionRepository;
    private final RegisterRepository registerRepository;
    private final RegistersMapper mapper;
    private final AuditService auditService;
    private final OutboxPublisher outbox;
    private final CurrentUser currentUser;
    private final ManagerStepUpAuthorizer managerAuthorizer;
    private final Clock clock = Clock.systemUTC();

    public TillSessionCommandService(PosTillSessionRepository sessionRepository,
                                     RegisterRepository registerRepository, RegistersMapper mapper,
                                     AuditService auditService, OutboxPublisher outbox,
                                     CurrentUser currentUser, ManagerStepUpAuthorizer managerAuthorizer) {
        this.sessionRepository = sessionRepository;
        this.registerRepository = registerRepository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.outbox = outbox;
        this.currentUser = currentUser;
        this.managerAuthorizer = managerAuthorizer;
    }

    /** FR-191 — open a till with an opening float. One OPEN till per register. */
    @Transactional
    public TillSessionResponse open(TillOpenRequest req) {
        Register register = registerRepository.findByPublicId(req.registerId())
            .orElseThrow(() -> DomainException.notFound("Register", req.registerId()));
        assertCanOpenSession(register);
        if (sessionRepository.findByRegisterIdAndStatus(register.getId(), TillSessionStatus.OPEN).isPresent()) {
            throw new DomainException(ErrorCode.CONFLICT, "A till is already open on this register");
        }

        String currency = req.currency() == null || req.currency().isBlank()
            ? DEFAULT_CURRENCY : req.currency().toUpperCase();
        Instant now = Instant.now(clock);

        PosTillSession session = new PosTillSession();
        session.setRegister(register);
        session.setLocationId(register.getLocationId());
        session.setCashierId(currentUser.optional().map(p -> p.userPublicId()).orElse(null));
        session.setStatus(TillSessionStatus.OPEN);
        session.setOpeningFloat(Money.ofMinor(req.openingFloat(), currency));
        session.setExpectedCash(Money.ofMinor(req.openingFloat(), currency));
        session.setOpenedAt(now);

        PosTillSession saved = sessionRepository.save(session);
        TillSessionResponse after = mapper.toResponse(saved);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null, after);
        outbox.publish(AUDIT_ENTITY, saved.getPublicId(), EVENT_TILL_OPENED, after);
        return after;
    }

    /** FR-192 — record a manual cash pickup / drop / payout (SALE/CHANGE are system-recorded only). */
    @Transactional
    public TillSessionResponse recordMovement(String tillPublicId, TillMovementRequest req) {
        PosTillSession session = load(tillPublicId);
        if (session.getStatus() != TillSessionStatus.OPEN) {
            throw new DomainException(ErrorCode.CONFLICT, "Till session is not open");
        }
        TillMovementType type;
        try {
            type = TillMovementType.valueOf(req.type());
        } catch (IllegalArgumentException e) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "Invalid movement type: " + req.type());
        }
        if (!MANUAL_MOVEMENT_TYPES.contains(type)) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Movement type " + type + " cannot be recorded manually");
        }

        long expected = expectedCash(session);
        if (type == TillMovementType.DROP && req.amount() > expected) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Drop amount exceeds the till's expected cash on hand");
        }

        String currency = session.getOpeningFloat().currency();
        PosTillMovement movement = new PosTillMovement();
        movement.setType(type);
        movement.setAmount(Money.ofMinor(req.amount(), currency));
        movement.setNote(req.note());
        session.addMovement(movement);
        session.setExpectedCash(Money.ofMinor(expectedCash(session), currency));

        PosTillSession saved = sessionRepository.save(session);
        TillSessionResponse after = mapper.toResponse(saved);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE,
            null, Map.of("movement", type.name(), "amount", req.amount(), "session", after));
        return after;
    }

    /**
     * FR-193 — close a till. A counted-cash variance over the threshold needs manager sign-off:
     * {@code managerUsername}/{@code managerPassword} are verified live against a real, active user
     * holding {@code pos.till.manage} (never the initiating cashier's own session) and the VERIFIED
     * manager's id is recorded as {@code varianceApprovedBy}.
     */
    @Transactional
    public TillSessionResponse close(String tillPublicId, TillCloseRequest req) {
        PosTillSession session = load(tillPublicId);
        if (session.getStatus() != TillSessionStatus.OPEN) {
            throw new DomainException(ErrorCode.CONFLICT, "Till session is not open");
        }
        long expected = expectedCash(session);
        long variance = req.countedCash() - expected;
        boolean overThreshold = Math.abs(variance) > CASH_VARIANCE_THRESHOLD_MINOR;

        String approverPublicId = null;
        if (overThreshold) {
            if (req.managerUsername() == null || req.managerUsername().isBlank()
                || req.managerPassword() == null || req.managerPassword().isBlank()) {
                throw new DomainException(ErrorCode.FORBIDDEN,
                    "Manager approval is required to close a till with a variance over the threshold");
            }
            approverPublicId = managerAuthorizer.authorize(
                req.managerUsername(), req.managerPassword(), PERMISSION_TILL_MANAGE);
        }

        String currency = session.getOpeningFloat().currency();
        session.setExpectedCash(Money.ofMinor(expected, currency));
        session.setCountedCash(Money.ofMinor(req.countedCash(), currency));
        session.setVarianceAmount(variance);
        if (overThreshold) {
            session.setVarianceApprovedBy(approverPublicId);
            session.setVarianceApprovedAt(Instant.now(clock));
        }
        session.setStatus(TillSessionStatus.CLOSED);
        session.setClosedAt(Instant.now(clock));

        PosTillSession saved = sessionRepository.save(session);
        TillSessionResponse after = mapper.toResponse(saved);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, null, after);
        outbox.publish(AUDIT_ENTITY, saved.getPublicId(), EVENT_TILL_CLOSED, Map.of(
            "session", after,
            "locationId", saved.getLocationId(),
            "registerId", saved.getRegister().getPublicId(),
            "entryDate", saved.getClosedAt() == null ? null : saved.getClosedAt().toString(),
            "cashierId", saved.getCashierId() == null ? "" : saved.getCashierId()));
        return after;
    }

    // --- internals -----------------------------------------------------------

    /** AC-005.1-4 / AC-005.1-5 — the register must be ACTIVE; MANAGER_ONLY requires the permission. */
    private void assertCanOpenSession(Register register) {
        if (register.getStatus() != RegisterStatus.ACTIVE) {
            throw new DomainException(ErrorCode.CONFLICT, "Register is disabled");
        }
        if (register.getOperatingMode() == RegisterOperatingMode.MANAGER_ONLY) {
            boolean hasPermission = currentUser.optional()
                .map(p -> p.permissions() != null && p.permissions().contains(PERMISSION_MANAGER_OPERATE))
                .orElse(false);
            if (!hasPermission) {
                throw new DomainException(ErrorCode.FORBIDDEN,
                    "This register is restricted to manager operation");
            }
        }
    }

    private long expectedCash(PosTillSession session) {
        long total = session.getOpeningFloat().amountMinor();
        for (PosTillMovement m : session.getMovements()) {
            if (CASH_IN.contains(m.getType())) {
                total += m.getAmount().amountMinor();
            } else if (CASH_OUT.contains(m.getType())) {
                total -= m.getAmount().amountMinor();
            }
        }
        return total;
    }

    private PosTillSession load(String publicId) {
        return sessionRepository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("PosTillSession", publicId));
    }
}

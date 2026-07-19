package com.guru.erp.modules.pos.registers.service;

import com.guru.erp.modules.pos.registers.domain.PosTillMovement;
import com.guru.erp.modules.pos.registers.domain.PosTillSession;
import com.guru.erp.modules.pos.registers.domain.Register;
import com.guru.erp.modules.pos.registers.domain.TillMovementType;
import com.guru.erp.modules.pos.registers.domain.TillSessionStatus;
import com.guru.erp.modules.pos.registers.dto.TillDtos.TillReportResponse;
import com.guru.erp.modules.pos.registers.dto.TillDtos.TillSessionResponse;
import com.guru.erp.modules.pos.registers.mapper.RegistersMapper;
import com.guru.erp.modules.pos.registers.repository.PosTillSessionRepository;
import com.guru.erp.modules.pos.registers.repository.RegisterRepository;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.web.PageResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-side use-cases for PosTillSession (US-037 FR-191..194): get, the
 * currently-open session for a register, list, and the X/Z till report.
 *
 * <p>The reference X/Z report also folds in gross-sales/refunds/transaction-count
 * from completed POS transactions on the session; the checkout sub-slice
 * (PosTransaction) has not been ported yet, so those fields report zero here
 * until that slice lands and this service is wired to it — the cash-movement
 * totals (opening float, pickups/drops/refund-outs, expected cash) are fully
 * accurate today since they are entirely till-session-local.
 */
@Service
public class TillSessionQueryService {

    private static final Set<TillMovementType> CASH_IN =
        EnumSet.of(TillMovementType.SALE, TillMovementType.PICKUP);
    private static final Set<TillMovementType> CASH_OUT =
        EnumSet.of(TillMovementType.DROP, TillMovementType.CHANGE, TillMovementType.REFUND_OUT);

    private final PosTillSessionRepository sessionRepository;
    private final RegisterRepository registerRepository;
    private final RegistersMapper mapper;
    private final Clock clock = Clock.systemUTC();

    public TillSessionQueryService(PosTillSessionRepository sessionRepository,
                                   RegisterRepository registerRepository, RegistersMapper mapper) {
        this.sessionRepository = sessionRepository;
        this.registerRepository = registerRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public TillSessionResponse get(String publicId) {
        return mapper.toResponse(load(publicId));
    }

    /** Currently OPEN session for a register, or {@code null} if none. */
    @Transactional(readOnly = true)
    public TillSessionResponse getOpenForRegister(String registerPublicId) {
        Register register = registerRepository.findByPublicId(registerPublicId)
            .orElseThrow(() -> DomainException.notFound("Register", registerPublicId));
        return sessionRepository.findByRegisterIdAndStatus(register.getId(), TillSessionStatus.OPEN)
            .map(mapper::toResponse)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public PageResponse<TillSessionResponse> list(String registerPublicId, String status, Pageable pageable) {
        TillSessionStatus st = status == null || status.isBlank() ? null : TillSessionStatus.valueOf(status);
        return PageResponse.of(sessionRepository.search(blankToNull(registerPublicId), st, pageable), mapper::toResponse);
    }

    /** FR-193 — X (mid-session, {@code reportType="X"}) or Z (end-of-day, {@code "Z"}) cash report. */
    @Transactional(readOnly = true)
    public TillReportResponse report(String tillPublicId, String reportType) {
        PosTillSession session = load(tillPublicId);
        List<PosTillMovement> movements = session.getMovements();
        long cashIn = movements.stream().filter(m -> CASH_IN.contains(m.getType()))
            .mapToLong(m -> m.getAmount().amountMinor()).sum();
        long cashOut = movements.stream().filter(m -> CASH_OUT.contains(m.getType()))
            .mapToLong(m -> m.getAmount().amountMinor()).sum();
        long expected = session.getExpectedCash() != null
            ? session.getExpectedCash().amountMinor()
            : session.getOpeningFloat().amountMinor() + cashIn - cashOut;

        return new TillReportResponse(
            session.getPublicId(),
            session.getRegister().getPublicId(),
            session.getLocationId(),
            reportType,
            session.getOpeningFloat().currency(),
            session.getOpeningFloat().amountMinor(),
            0L, // gross sales — pending checkout sub-slice
            0L, // refunds — pending checkout sub-slice
            0L, // net sales — pending checkout sub-slice
            cashIn,
            cashOut,
            expected,
            session.getCountedCash() == null ? null : session.getCountedCash().amountMinor(),
            session.getVarianceAmount(),
            0L, // transaction count — pending checkout sub-slice
            session.getOpenedAt(),
            Instant.now(clock),
            movements.stream().map(mapper::toResponse).toList());
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private PosTillSession load(String publicId) {
        return sessionRepository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("PosTillSession", publicId));
    }
}

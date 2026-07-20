package com.springboot.erp.modules.finance.periods.service;

import com.springboot.erp.modules.finance.periods.domain.FiscalPeriod;
import com.springboot.erp.modules.finance.periods.domain.PeriodSnapshot;
import com.springboot.erp.modules.finance.periods.dto.PeriodSnapshotDtos.PeriodSnapshotResponse;
import com.springboot.erp.modules.finance.periods.mapper.PeriodSnapshotMapper;
import com.springboot.erp.modules.finance.periods.repository.GlPeriodQueryRepository;
import com.springboot.erp.modules.finance.periods.repository.PeriodSnapshotRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E-009 Phase 5 — freezes an immutable financial snapshot when a {@link FiscalPeriod} transitions
 * CLOSING -&gt; CLOSED (reference {@code PeriodSnapshotView.create_snapshot}). Trial-balance only for
 * now — P&amp;L / balance-sheet report builders don't exist yet in this port either (same MVP caveat
 * as the reference); the payload shape leaves room to add them later. Every call APPENDS a new
 * {@link PeriodSnapshot} row (versioned) — nothing here ever updates a prior snapshot, so a reopen
 * -&gt; re-close keeps every prior run on record for audit. This is a read-only historical record once
 * written.
 */
@Service
public class PeriodSnapshotService {

    private static final String AUDIT_ENTITY = "period_snapshot";

    private final PeriodSnapshotRepository repository;
    private final GlPeriodQueryRepository glRepository;
    private final PeriodSnapshotMapper mapper;
    private final AuditService auditService;

    public PeriodSnapshotService(PeriodSnapshotRepository repository, GlPeriodQueryRepository glRepository,
                                 PeriodSnapshotMapper mapper, AuditService auditService) {
        this.repository = repository;
        this.glRepository = glRepository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional
    public PeriodSnapshot createSnapshot(FiscalPeriod period, String actorId) {
        List<Object[]> rows = glRepository.sumPostedDebitCredit(period.getCompanyId(), period.getDateTo());
        long debit = rows.isEmpty() ? 0L : ((Number) rows.get(0)[0]).longValue();
        long credit = rows.isEmpty() ? 0L : ((Number) rows.get(0)[1]).longValue();
        Map<String, Object> trialBalance = Map.of(
            "totalDebit", debit,
            "totalCredit", credit,
            "balanced", debit == credit,
            "asOf", period.getDateTo().toString());

        int nextVersion = repository.findTopByFiscalPeriodIdOrderByVersionNoDesc(period.getId())
            .map(s -> s.getVersionNo() + 1)
            .orElse(1);

        PeriodSnapshot snapshot = new PeriodSnapshot();
        snapshot.setFiscalPeriod(period);
        snapshot.setGeneratedBy(actorId);
        snapshot.setPayload(Map.of("trialBalance", trialBalance));
        snapshot.setVersionNo(nextVersion);
        PeriodSnapshot saved = repository.save(snapshot);

        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null,
            Map.of("periodCode", period.getPeriodCode(), "versionNo", saved.getVersionNo()));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PeriodSnapshotResponse> listSnapshots(FiscalPeriod period) {
        return repository.findByFiscalPeriodIdOrderByVersionNoDesc(period.getId()).stream()
            .map(mapper::toResponse)
            .toList();
    }
}

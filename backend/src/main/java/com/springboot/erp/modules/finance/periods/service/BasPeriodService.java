package com.springboot.erp.modules.finance.periods.service;

import com.springboot.erp.modules.finance.periods.domain.BasPeriod;
import com.springboot.erp.modules.finance.periods.domain.BasPeriodStatus;
import com.springboot.erp.modules.finance.periods.dto.BasDtos.BasPeriodCreateRequest;
import com.springboot.erp.modules.finance.periods.dto.BasDtos.BasPeriodResponse;
import com.springboot.erp.modules.finance.periods.dto.BasDtos.BasPeriodTransitionRequest;
import com.springboot.erp.modules.finance.periods.mapper.BasMapper;
import com.springboot.erp.modules.finance.periods.repository.BasPeriodRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.security.CurrentUser;
import com.springboot.erp.platform.status.StateMachine;
import com.springboot.erp.platform.web.PageResponse;
import java.time.Instant;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ENT-AU-001 BasPeriod CRUD + lifecycle (reference {@code BasPeriodsView}). OPEN -&gt; LODGED -&gt;
 * FROZEN, plus the sanctioned un-lodge (LODGED -&gt; OPEN) and unfreeze (FROZEN -&gt; OPEN) overrides.
 * Unfreezing specifically requires the elevated {@code finance.period.adjust} permission — the
 * platform {@link StateMachine} only knows a move is topologically legal, not who may make it, so
 * that permission check stays here in the service.
 */
@Service
public class BasPeriodService {

    private static final String AUDIT_ENTITY = "bas_period";
    private static final String PERMISSION_PERIOD_ADJUST = "finance.period.adjust";

    static final StateMachine<BasPeriodStatus> WORKFLOW = StateMachine.builder(BasPeriodStatus.class)
        .allow(BasPeriodStatus.OPEN, BasPeriodStatus.LODGED)
        .allow(BasPeriodStatus.LODGED, BasPeriodStatus.FROZEN, BasPeriodStatus.OPEN)
        .allow(BasPeriodStatus.FROZEN, BasPeriodStatus.OPEN)
        .build();

    private final BasPeriodRepository repository;
    private final BasMapper mapper;
    private final AuditService auditService;
    private final CurrentUser currentUser;

    public BasPeriodService(BasPeriodRepository repository, BasMapper mapper, AuditService auditService,
                            CurrentUser currentUser) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public PageResponse<BasPeriodResponse> list(String companyId, BasPeriodStatus status, Pageable pageable) {
        return PageResponse.of(repository.search(companyId, status, pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public BasPeriodResponse get(String publicId) {
        return mapper.toResponse(require(publicId));
    }

    public BasPeriod require(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("BasPeriod", publicId));
    }

    @Transactional
    public BasPeriodResponse create(BasPeriodCreateRequest req) {
        if (repository.existsByCompanyIdAndPeriodCode(req.companyId(), req.periodCode())) {
            throw new DomainException(ErrorCode.DUPLICATE,
                "A BAS period with code '" + req.periodCode() + "' already exists for this company");
        }
        BasPeriod period = new BasPeriod();
        period.setCompanyId(req.companyId());
        period.setPeriodCode(req.periodCode());
        period.setPeriodType(req.periodType());
        period.setDateFrom(req.dateFrom());
        period.setDateTo(req.dateTo());
        period.setGstAccountId(req.gstAccountId());
        period.setRevenueAccountId(req.revenueAccountId());
        period.setStatus(BasPeriodStatus.OPEN);
        BasPeriod saved = repository.save(period);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null, mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public BasPeriodResponse transition(String publicId, BasPeriodTransitionRequest req) {
        BasPeriod period = require(publicId);
        BasPeriodStatus from = period.getStatus();
        BasPeriodStatus to = req.newStatus();
        if (from == to) {
            return mapper.toResponse(period);
        }

        // Unfreezing (FROZEN -> OPEN) is a deliberate override — requires the elevated permission,
        // same as reopening a closed fiscal period.
        if (from == BasPeriodStatus.FROZEN
            && !currentUser.optional().map(p -> p.permissions().contains(PERMISSION_PERIOD_ADJUST)).orElse(false)) {
            throw new DomainException(ErrorCode.FORBIDDEN,
                "Unfreezing a BAS period requires the finance.period.adjust permission");
        }

        BasPeriodResponse before = mapper.toResponse(period);
        period.setStatus(WORKFLOW.transition(from, to));
        if (to == BasPeriodStatus.LODGED) {
            period.setLodgedAt(Instant.now());
            period.setLodgedBy(actorId());
            if (req.lodgementReference() != null) {
                period.setLodgementReference(req.lodgementReference());
            }
        }
        BasPeriod saved = repository.save(period);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before, mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    private String actorId() {
        return currentUser.optional().map(p -> p.userPublicId()).orElse(null);
    }
}

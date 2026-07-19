package com.guru.erp.modules.finance.periods.service;

import com.guru.erp.modules.finance.periods.domain.FiscalPeriod;
import com.guru.erp.modules.finance.periods.domain.FiscalPeriodStatus;
import com.guru.erp.modules.finance.periods.domain.PostingPolicy;
import com.guru.erp.modules.finance.periods.repository.FiscalPeriodRepository;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import com.guru.erp.platform.security.CurrentUser;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AC-043-3 / AC-048-1 / AC-048-2 posting gate (reference {@code
 * fiscal_periods_view.assert_period_can_post}). Any module about to post a GL journal entry calls
 * {@link #assertCanPost} first, resolving the fiscal period that contains the document date and
 * checking whether the caller may post into it given that period's lifecycle status.
 */
@Service
@Transactional(readOnly = true)
public class PeriodPostingGuardService {

    private static final String PERMISSION_PERIOD_ADJUST = "finance.period.adjust";

    /** Reference {@code _POSTING_POLICY} in {@code app.finance.views.fiscal_periods}. */
    private static final Map<FiscalPeriodStatus, PostingPolicy> POLICY = new EnumMap<>(FiscalPeriodStatus.class);

    static {
        POLICY.put(FiscalPeriodStatus.DRAFT, PostingPolicy.BLOCKED);
        POLICY.put(FiscalPeriodStatus.OPEN, PostingPolicy.ALLOWED);
        POLICY.put(FiscalPeriodStatus.PREPARING, PostingPolicy.ALLOWED);
        POLICY.put(FiscalPeriodStatus.RECONCILING, PostingPolicy.ALLOWED);
        POLICY.put(FiscalPeriodStatus.VALIDATING, PostingPolicy.ALLOWED);
        POLICY.put(FiscalPeriodStatus.PENDING_APPROVAL, PostingPolicy.ALLOWED);
        POLICY.put(FiscalPeriodStatus.APPROVED, PostingPolicy.ALLOWED);
        POLICY.put(FiscalPeriodStatus.CLOSING, PostingPolicy.ALLOWED);
        POLICY.put(FiscalPeriodStatus.CLOSED, PostingPolicy.BLOCKED);
        POLICY.put(FiscalPeriodStatus.ADJUSTMENT, PostingPolicy.ADJUST_ONLY);
        POLICY.put(FiscalPeriodStatus.ARCHIVED, PostingPolicy.BLOCKED);
    }

    private final FiscalPeriodRepository repository;
    private final CurrentUser currentUser;

    public PeriodPostingGuardService(FiscalPeriodRepository repository, CurrentUser currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
    }

    /** Returns the containing {@link FiscalPeriod} if posting is allowed, else throws 409/404. */
    public FiscalPeriod assertCanPost(String companyId, LocalDate documentDate) {
        FiscalPeriod period = repository.findContaining(companyId, documentDate)
            .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND,
                "No fiscal period covers document date " + documentDate));

        PostingPolicy policy = POLICY.getOrDefault(period.getStatus(), PostingPolicy.BLOCKED);
        if (policy == PostingPolicy.ALLOWED) {
            return period;
        }
        if (policy == PostingPolicy.ADJUST_ONLY
            && currentUser.optional().map(p -> p.permissions().contains(PERMISSION_PERIOD_ADJUST)).orElse(false)) {
            return period;
        }
        throw new DomainException(ErrorCode.CONFLICT,
            "Fiscal period '" + period.getPeriodCode() + "' is " + period.getStatus() + " and not open for posting");
    }
}

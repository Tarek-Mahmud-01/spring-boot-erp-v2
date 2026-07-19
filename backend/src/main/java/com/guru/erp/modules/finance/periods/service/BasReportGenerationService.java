package com.guru.erp.modules.finance.periods.service;

import com.guru.erp.modules.finance.periods.domain.BasPeriod;
import com.guru.erp.modules.finance.periods.domain.BasPeriodStatus;
import com.guru.erp.modules.finance.periods.domain.BasReport;
import com.guru.erp.modules.finance.periods.dto.BasDtos.BasReportSnapshotResponse;
import com.guru.erp.modules.finance.periods.mapper.BasMapper;
import com.guru.erp.modules.finance.periods.repository.BasReportRepository;
import com.guru.erp.modules.finance.periods.repository.GlPeriodQueryRepository;
import com.guru.erp.platform.audit.AuditAction;
import com.guru.erp.platform.audit.AuditService;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import com.guru.erp.platform.security.CurrentUser;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-AU-020/021 — computes the BAS boxes for a {@link BasPeriod} and persists a versioned,
 * immutable {@link BasReport} snapshot (reference {@code BasPeriodsView.generate_bas_report} +
 * {@code BasReportView.summary}).
 *
 * <p>Sale-side voucher types (POS sale/refund/void — reference {@code POS_SALE_VOUCHER_TYPE} /
 * {@code POS_REFUND_VOUCHER_TYPE} / {@code POS_VOID_VOUCHER_TYPE}, {@code V-001}/{@code V-008}/
 * {@code V-005}) split the GST account's activity into 1A (sales-side GST adjustment); every other
 * voucher type nets to 1B (purchase input credit) — same split logic as the reference. G1 nets the
 * revenue account's credits against debits.
 *
 * <p><b>Scope note</b>: this slice computes every box strictly from POSTED GL journal lines (own
 * GST/revenue account, explicit on {@code BasPeriod}). The reference's fuller {@code bas_report()}
 * additionally derives G2 (export sales) / G3 (GST-free sales) / G10 (capital purchases) / G11
 * (non-capital purchases) from raw POS transaction lines and supplier bill lines with their own
 * per-line tax-code / capital-item flags — those source documents belong to the pos/procurement
 * modules, out of scope for this finance/periods slice. G2/G3/G10/G11 are therefore always 0 here;
 * a documented scope decision, not a bug — a future cross-module BAS aggregator can extend this
 * service once those slices are ported.
 */
@Service
public class BasReportGenerationService {

    private static final String AUDIT_ENTITY = "bas_report";

    /** Reference {@code _SALE_SIDE_VOUCHER_TYPES}. */
    private static final Set<String> SALE_SIDE_VOUCHER_TYPES = Set.of("V-001", "V-008", "V-005");

    private final BasReportRepository repository;
    private final GlPeriodQueryRepository glRepository;
    private final BasMapper mapper;
    private final AuditService auditService;
    private final CurrentUser currentUser;

    public BasReportGenerationService(BasReportRepository repository, GlPeriodQueryRepository glRepository,
                                      BasMapper mapper, AuditService auditService, CurrentUser currentUser) {
        this.repository = repository;
        this.glRepository = glRepository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    @Transactional
    public BasReportSnapshotResponse generate(BasPeriod period) {
        if (period.getStatus() == BasPeriodStatus.FROZEN) {
            throw new DomainException(ErrorCode.CONFLICT,
                "BAS period '" + period.getPeriodCode() + "' is FROZEN and cannot be regenerated");
        }

        Map<String, long[]> gstByVoucherType = byVoucherType(period, period.getGstAccountId());
        Map<String, long[]> revenueByVoucherType = byVoucherType(period, period.getRevenueAccountId());

        long a1 = 0L; // GST on sales
        long b1 = 0L; // GST on purchases
        for (Map.Entry<String, long[]> e : gstByVoucherType.entrySet()) {
            long debit = e.getValue()[0];
            long credit = e.getValue()[1];
            if (SALE_SIDE_VOUCHER_TYPES.contains(e.getKey())) {
                a1 += credit - debit;
            } else {
                b1 += debit - credit;
            }
        }
        long revenueNet = revenueByVoucherType.values().stream().mapToLong(v -> v[1] - v[0]).sum();
        long g1 = revenueNet + a1;

        Map<String, Long> boxValues = new HashMap<>();
        boxValues.put("G1", g1);
        boxValues.put("G2", 0L);
        boxValues.put("G3", 0L);
        boxValues.put("G10", 0L);
        boxValues.put("G11", 0L);
        boxValues.put("1A", a1);
        boxValues.put("1B", b1);
        boxValues.put("netGst", a1 - b1);

        int nextVersion = repository.findTopByBasPeriodIdOrderByVersionNoDesc(period.getId())
            .map(r -> r.getVersionNo() + 1)
            .orElse(1);

        BasReport report = new BasReport();
        report.setBasPeriod(period);
        report.setGeneratedBy(currentUser.optional().map(p -> p.userPublicId()).orElse(null));
        report.setBoxValues(boxValues);
        report.setVersionNo(nextVersion);
        BasReport saved = repository.save(report);

        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null,
            Map.of("basPeriodId", period.getPublicId(), "versionNo", saved.getVersionNo()));
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BasReportSnapshotResponse> listReports(BasPeriod period) {
        return repository.findByBasPeriodIdOrderByVersionNoDesc(period.getId()).stream()
            .map(mapper::toResponse)
            .toList();
    }

    /** {voucherType -&gt; [baseDebit, baseCredit]} for POSTED lines on one account in the period's range. */
    private Map<String, long[]> byVoucherType(BasPeriod period, String accountId) {
        if (accountId == null) {
            return Map.of();
        }
        List<Object[]> rows = glRepository.sumByVoucherTypeForAccount(
            period.getCompanyId(), accountId, period.getDateFrom(), period.getDateTo());
        Map<String, long[]> result = new HashMap<>();
        for (Object[] row : rows) {
            String voucherType = (String) row[0];
            long debit = ((Number) row[1]).longValue();
            long credit = ((Number) row[2]).longValue();
            result.put(voucherType, new long[] {debit, credit});
        }
        return result;
    }
}

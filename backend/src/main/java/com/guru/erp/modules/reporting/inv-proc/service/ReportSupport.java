package com.guru.erp.modules.reporting.invproc.service;

import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Shared date/BigDecimal/predicate helpers for every query service in this reporting sub-slice
 * (mirrors {@code reporting.finance.service.FinanceReportSupport}).
 */
@Component
public class ReportSupport {

    /** Calendar date -> inclusive UTC start-of-day instant. */
    public Instant startOfDay(LocalDate d) {
        return d == null ? null : d.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /** Calendar date -> inclusive UTC end-of-day instant (23:59:59.999999). */
    public Instant endOfDay(LocalDate d) {
        return d == null ? null : d.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1000);
    }

    /** Optional [from, to] date-range sanity check — to must be on/after from when both are set. */
    public void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "toDate must be on or after fromDate");
        }
    }

    /** AND every predicate in the list together; empty list -> always-true. */
    public BooleanExpression and(List<BooleanExpression> conds) {
        BooleanExpression result = null;
        for (BooleanExpression c : conds) {
            result = result == null ? c : result.and(c);
        }
        return result == null ? Expressions.TRUE.isTrue() : result;
    }

    public BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** HALF_EVEN round to a long minor-unit amount (ARCHITECTURE.md §5 — banker's rounding). */
    public long roundMinor(BigDecimal v) {
        return nz(v).setScale(0, RoundingMode.HALF_EVEN).longValueExact();
    }

    /** Full-precision average = value / qty, or ZERO when qty is zero. */
    public BigDecimal avg(BigDecimal value, BigDecimal qty) {
        BigDecimal q = nz(qty);
        return q.signum() == 0 ? BigDecimal.ZERO : nz(value).divide(q, 10, RoundingMode.HALF_EVEN);
    }
}

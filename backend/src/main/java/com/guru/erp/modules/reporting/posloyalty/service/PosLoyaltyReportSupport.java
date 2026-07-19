package com.guru.erp.modules.reporting.posloyalty.service;

import com.guru.erp.modules.settings.company.domain.QCompany;
import com.guru.erp.modules.settings.location.domain.QLocation;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Shared scope-resolution helpers for every pos-loyalty report query service (mirrors
 * {@code reporting.finance.service.FinanceReportSupport}).
 *
 * <p>{@code company_id} is a loose cross-module ULID everywhere in this codebase — reports filter
 * on the string id directly. The one exception is POS/CRM data that only carries a {@code
 * location_id} (no {@code company_id} column of its own — see {@code PosTransaction},
 * {@code PosTillSession}, {@code Register}, {@code CustomerTransaction}): those rows are scoped to
 * a company by first resolving the set of {@code Location.publicId}s owned by that company
 * (settings.location DOES keep a real {@code company} FK, since both live in the settings module),
 * then filtering the POS/CRM row's loose {@code location_id} column against that set with an
 * {@code IN} predicate. A single {@code location_id} filter is compared directly as a ULID string
 * (loose reference, no existence probe — a typo'd id just yields an empty report).
 */
@Component
public class PosLoyaltyReportSupport {

    private final JPAQueryFactory queryFactory;

    public PosLoyaltyReportSupport(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    /** Validate an optional [fromDate, toDate] range: to must be on/after from when both are set. */
    public void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && toDate.isBefore(fromDate)) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "toDate must be on or after fromDate");
        }
    }

    /**
     * Every {@code Location.publicId} owned by this company — the join key used to scope POS/CRM
     * rows that only carry a loose {@code location_id} column. Empty (never null) when the company
     * has no locations, so callers can safely build an {@code IN} predicate over it (an empty IN
     * predicate is short-circuited by the caller, never sent to the DB).
     */
    public List<String> locationIdsForCompany(String companyPublicId) {
        QLocation location = QLocation.location;
        return queryFactory
            .select(location.publicId)
            .from(location)
            .where(location.company.publicId.eq(companyPublicId))
            .fetch();
    }

    /**
     * The company's base currency (ISO-4217), or {@code "USD"} when the company ULID does not
     * resolve (loose cross-module reference — a typo'd id should never 404 a report, consistent
     * with every other cross-module ULID filter in this codebase).
     */
    public String baseCurrency(String companyPublicId) {
        QCompany company = QCompany.company;
        String currency = queryFactory
            .select(company.baseCurrency)
            .from(company)
            .where(company.publicId.eq(companyPublicId))
            .fetchOne();
        return currency == null ? "USD" : currency;
    }

    /** UTC start-of-day instant for an inclusive {@code fromDate} filter. */
    public java.time.Instant startOfDayUtc(LocalDate d) {
        return d.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /** UTC end-of-day (23:59:59.999999999) instant for an inclusive {@code toDate} filter. */
    public java.time.Instant endOfDayUtc(LocalDate d) {
        return d.atTime(23, 59, 59, 999_999_999).atZone(ZoneOffset.UTC).toInstant();
    }
}

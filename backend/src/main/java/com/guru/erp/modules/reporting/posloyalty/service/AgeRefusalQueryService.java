package com.guru.erp.modules.reporting.posloyalty.service;

import com.guru.erp.modules.access.domain.QUser;
import com.guru.erp.modules.pos.auxiliary.domain.PosEventType;
import com.guru.erp.modules.pos.auxiliary.domain.QPosEvent;
import com.guru.erp.modules.pos.registers.domain.QRegister;
import com.guru.erp.modules.reporting.posloyalty.dto.PosOpsReportDtos.AgeRefusalResponse;
import com.guru.erp.modules.reporting.posloyalty.dto.PosOpsReportDtos.AgeRefusalRow;
import com.guru.erp.modules.reporting.posloyalty.dto.PosOpsReportDtos.AgeRefusalSummary;
import com.guru.erp.platform.web.PageResponse;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RPT-AU-004 — Age Verification Refusals (reference {@code repositories/pos_ops.py::age_refusals}).
 *
 * <p>Refusal events carry no transaction (the restricted line is dropped before checkout — see the
 * reference's {@code record_age_refusal}), so this scopes by company via {@code Register ->
 * Location} instead of the {@code PosTransaction} join every other report in this sub-slice uses.
 */
@Service
@Transactional(readOnly = true)
public class AgeRefusalQueryService {

    private final JPAQueryFactory queryFactory;
    private final PosLoyaltyReportSupport support;

    public AgeRefusalQueryService(JPAQueryFactory queryFactory, PosLoyaltyReportSupport support) {
        this.queryFactory = queryFactory;
        this.support = support;
    }

    public AgeRefusalResponse ageRefusals(String companyId, String cashierId, String locationId,
                                         LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        support.validateDateRange(fromDate, toDate);
        List<String> companyLocationIds = support.locationIdsForCompany(companyId);
        if (companyLocationIds.isEmpty()) {
            return new AgeRefusalResponse(PageResponse.of(Page.empty(pageable)), new AgeRefusalSummary(companyId, 0));
        }

        QPosEvent event = QPosEvent.posEvent;
        QRegister register = QRegister.register;
        QUser actor = QUser.user;

        BooleanExpression predicate = event.type.eq(PosEventType.AGE_VERIFICATION_REFUSED)
            .and(register.locationId.in(companyLocationIds));
        if (locationId != null) {
            predicate = predicate.and(register.locationId.eq(locationId));
        }
        if (cashierId != null) {
            predicate = predicate.and(actor.publicId.eq(cashierId));
        }
        if (fromDate != null) {
            predicate = predicate.and(event.createdAt.goe(support.startOfDayUtc(fromDate)));
        }
        if (toDate != null) {
            predicate = predicate.and(event.createdAt.loe(support.endOfDayUtc(toDate)));
        }

        var baseQuery = queryFactory
            .select(event.publicId, event.createdAt, event.payload, register.locationId, actor.publicId, actor.fullName)
            .from(event)
            .join(register).on(register.publicId.eq(event.registerId))
            .leftJoin(actor).on(actor.publicId.eq(event.createdBy))
            .where(predicate);

        long total = baseQuery.fetchCount();
        List<Tuple> tuples = baseQuery
            .orderBy(event.createdAt.desc(), event.id.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        List<AgeRefusalRow> rows = tuples.stream().map(t -> {
            Map<String, Object> payload = t.get(event.payload);
            payload = payload == null ? Map.of() : payload;
            return new AgeRefusalRow(
                t.get(event.publicId),
                t.get(event.createdAt),
                t.get(actor.publicId) == null ? "" : t.get(actor.publicId),
                t.get(actor.fullName) == null || t.get(actor.fullName).isBlank() ? "—" : t.get(actor.fullName),
                (String) payload.get("product_id"),
                (String) payload.get("sku"),
                (String) payload.get("reason"),
                (String) payload.get("id_type"),
                t.get(register.locationId));
        }).toList();

        Page<AgeRefusalRow> page = new PageImpl<>(rows, pageable, total);
        return new AgeRefusalResponse(PageResponse.of(page), new AgeRefusalSummary(companyId, total));
    }
}

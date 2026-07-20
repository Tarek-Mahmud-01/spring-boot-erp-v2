package com.springboot.erp.modules.reporting.posloyalty.service;

import com.springboot.erp.modules.crm.customers.domain.CustomerStatus;
import com.springboot.erp.modules.crm.customers.domain.QCustomer;
import com.springboot.erp.modules.crm.customers.domain.QCustomerProfile;
import com.springboot.erp.modules.crm.customers.domain.QCustomerSegment;
import com.springboot.erp.modules.crm.customers.domain.QCustomerSegmentMember;
import com.springboot.erp.modules.crm.loyalty.domain.QLoyaltyAccount;
import com.springboot.erp.modules.reporting.posloyalty.dto.LoyaltyReportDtos.ConsentRow;
import com.springboot.erp.modules.reporting.posloyalty.dto.LoyaltyReportDtos.CustomersWithConsentResponse;
import com.springboot.erp.modules.reporting.posloyalty.dto.LoyaltyReportDtos.CustomersWithConsentSummary;
import com.springboot.erp.platform.web.PageResponse;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RPT-021 — Customer List with Consent (reference {@code repositories/loyalty.py::customers_with_consent}).
 * Consent-aware marketing extract: email/sms/analytics opt-in flags, segment codes, and rolling
 * 12-month spend, filtered by consent mode and free-text search.
 */
@Service
@Transactional(readOnly = true)
public class CustomerConsentQueryService {

    private final JPAQueryFactory queryFactory;
    private final PosLoyaltyReportSupport support;

    public CustomerConsentQueryService(JPAQueryFactory queryFactory, PosLoyaltyReportSupport support) {
        this.queryFactory = queryFactory;
        this.support = support;
    }

    public CustomersWithConsentResponse customersWithConsent(String companyId, String consent, String search,
                                                             boolean activeOnly, Pageable pageable) {
        String baseCurrency = support.baseCurrency(companyId);
        QCustomer customer = QCustomer.customer;
        QCustomerProfile profile = QCustomerProfile.customerProfile;
        QLoyaltyAccount account = QLoyaltyAccount.loyaltyAccount;

        BooleanExpression predicate = customer.companyId.eq(companyId);
        if (activeOnly) {
            predicate = predicate.and(customer.status.eq(CustomerStatus.ACTIVE));
        }
        String consentUpper = consent == null ? null : consent.toUpperCase(Locale.ROOT);
        if ("EMAIL".equals(consentUpper)) {
            predicate = predicate.and(profile.consentEmail.isTrue());
        } else if ("SMS".equals(consentUpper)) {
            predicate = predicate.and(profile.consentSms.isTrue());
        } else if ("ANY".equals(consentUpper)) {
            predicate = predicate.and(profile.consentEmail.isTrue().or(profile.consentSms.isTrue()));
        } else if ("NONE".equals(consentUpper)) {
            predicate = predicate.and(profile.consentEmail.isFalse().and(profile.consentSms.isFalse()));
        }
        if (search != null && !search.isBlank()) {
            String term = search.strip();
            predicate = predicate.and(customer.firstName.containsIgnoreCase(term)
                .or(customer.lastName.containsIgnoreCase(term))
                .or(customer.membershipId.containsIgnoreCase(term))
                .or(profile.email.containsIgnoreCase(term))
                .or(profile.mobile.containsIgnoreCase(term)));
        }

        var baseQuery = queryFactory
            .select(customer.id, customer.publicId, customer.membershipId, customer.firstName, customer.lastName,
                customer.analyticsConsent, profile.email, profile.mobile, profile.postcode, profile.consentEmail,
                profile.consentSms, profile.consentEmailAt, profile.consentSmsAt, account.rolling12mSpendAmount,
                account.spendCurrency)
            .from(customer)
            .join(profile).on(profile.customer.id.eq(customer.id))
            .leftJoin(account).on(account.customerId.eq(customer.publicId))
            .where(predicate);

        long total = baseQuery.fetchCount();
        List<Tuple> tuples = baseQuery
            .orderBy(customer.membershipId.asc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        List<Long> pageIds = tuples.stream().map(t -> t.get(customer.id)).toList();
        Map<Long, List<String>> segmentCodes = new LinkedHashMap<>();
        if (!pageIds.isEmpty()) {
            QCustomerSegmentMember member = QCustomerSegmentMember.customerSegmentMember;
            QCustomerSegment segment = QCustomerSegment.customerSegment;
            for (Tuple t : queryFactory
                .select(member.customer.id, segment.code)
                .from(member)
                .join(segment).on(segment.id.eq(member.segment.id))
                .where(member.customer.id.in(pageIds))
                .orderBy(segment.code.asc())
                .fetch()) {
                segmentCodes.computeIfAbsent(t.get(member.customer.id), k -> new ArrayList<>()).add(t.get(segment.code));
            }
        }

        List<ConsentRow> rows = new ArrayList<>();
        for (Tuple t : tuples) {
            Instant emailAt = t.get(profile.consentEmailAt);
            Instant smsAt = t.get(profile.consentSmsAt);
            Instant last = maxOf(emailAt, smsAt);
            String name = (t.get(customer.firstName) + " " + t.get(customer.lastName)).strip();
            rows.add(new ConsentRow(
                t.get(customer.publicId),
                t.get(customer.membershipId),
                name,
                t.get(profile.email),
                t.get(profile.mobile),
                t.get(profile.postcode),
                segmentCodes.getOrDefault(t.get(customer.id), List.of()),
                Boolean.TRUE.equals(t.get(profile.consentEmail)),
                Boolean.TRUE.equals(t.get(profile.consentSms)),
                Boolean.TRUE.equals(t.get(customer.analyticsConsent)),
                last,
                orZero(t.get(account.rolling12mSpendAmount)),
                t.get(account.spendCurrency) == null ? baseCurrency : t.get(account.spendCurrency)));
        }
        Page<ConsentRow> page = new PageImpl<>(rows, pageable, total);

        var summaryTuple = queryFactory
            .select(customer.id.count(),
                Expressions.numberTemplate(Long.class, "sum(case when {0} = true then 1 else 0 end)", profile.consentEmail),
                Expressions.numberTemplate(Long.class, "sum(case when {0} = true then 1 else 0 end)", profile.consentSms),
                Expressions.numberTemplate(Long.class, "sum(case when {0} = true then 1 else 0 end)", customer.analyticsConsent))
            .from(customer)
            .join(profile).on(profile.customer.id.eq(customer.id))
            .where(predicate)
            .fetchOne();

        CustomersWithConsentSummary summary = summaryTuple == null
            ? new CustomersWithConsentSummary(companyId, 0, 0, 0, 0)
            : new CustomersWithConsentSummary(companyId, orZero(summaryTuple.get(0, Long.class)),
                orZero(summaryTuple.get(1, Long.class)), orZero(summaryTuple.get(2, Long.class)),
                orZero(summaryTuple.get(3, Long.class)));
        return new CustomersWithConsentResponse(PageResponse.of(page), summary);
    }

    private static long orZero(Long v) {
        return v == null ? 0L : v;
    }

    private static Instant maxOf(Instant a, Instant b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
    }
}

package com.springboot.erp.modules.reporting.posloyalty.service;

import com.springboot.erp.modules.crm.loyalty.domain.QLoyaltyConfig;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Flattens a company's {@code LoyaltyConfig} into the shape the loyalty-analytics reports need
 * (reference {@code repositories/loyalty_analytics.py::_load_program} / {@code _tier_name_map} /
 * {@code _points_to_minor}). Shared by RPT-028/029/030/031 so each query service stays focused on
 * its own aggregation.
 */
@Component
public class LoyaltyProgramSupport {

    private final JPAQueryFactory queryFactory;
    private final PosLoyaltyReportSupport support;

    public LoyaltyProgramSupport(JPAQueryFactory queryFactory, PosLoyaltyReportSupport support) {
        this.queryFactory = queryFactory;
        this.support = support;
    }

    /** Flattened program config; {@code pointsPerCurrencyUnit} / {@code expiryMonths} default to 0 (never expires / no conversion) when the company has no config row yet. */
    public record Program(String currency, int pointsPerCurrencyUnit, int expiryMonths, List<Map<String, Object>> tiers) {
    }

    public Program loadProgram(String companyId) {
        QLoyaltyConfig config = QLoyaltyConfig.loyaltyConfig;
        String baseCurrency = support.baseCurrency(companyId);
        var row = queryFactory
            .select(config.currency, config.redeemRule, config.expiryMonths, config.tiers)
            .from(config)
            .where(config.companyId.eq(companyId))
            .fetchOne();
        if (row == null) {
            return new Program(baseCurrency, 0, 0, List.of());
        }
        Map<String, Object> redeem = row.get(config.redeemRule);
        redeem = redeem == null ? Map.of() : redeem;
        Object perUnit = redeem.get("pointsPerCurrencyUnit");
        int pointsPerCurrencyUnit = perUnit == null ? 0 : ((Number) perUnit).intValue();
        Integer expiryMonths = row.get(config.expiryMonths);
        String currency = row.get(config.currency);
        List<Map<String, Object>> tiers = row.get(config.tiers);
        return new Program(currency == null ? baseCurrency : currency, pointsPerCurrencyUnit,
            expiryMonths == null ? 0 : expiryMonths, tiers == null ? List.of() : tiers);
    }

    /** Points -> minor-currency-units conversion (reference {@code _points_to_minor}); 0 when the program has no conversion rate configured. */
    public long pointsToMinor(long points, int pointsPerCurrencyUnit) {
        if (pointsPerCurrencyUnit <= 0) {
            return 0L;
        }
        return Math.round(points / (double) pointsPerCurrencyUnit * 100);
    }

    /** {@code tierId -> tierName} lookup built from the program's tier list. */
    public Map<String, String> tierNameMap(Program program) {
        Map<String, String> map = new java.util.HashMap<>();
        for (Map<String, Object> t : program.tiers()) {
            Object id = t.get("id");
            Object name = t.get("name");
            if (id != null) {
                map.put(String.valueOf(id), name == null ? "—" : String.valueOf(name));
            }
        }
        return map;
    }
}

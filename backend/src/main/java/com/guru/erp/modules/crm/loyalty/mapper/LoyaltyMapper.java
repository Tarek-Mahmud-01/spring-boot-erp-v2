package com.guru.erp.modules.crm.loyalty.mapper;

import com.guru.erp.modules.crm.loyalty.domain.CustomerTransaction;
import com.guru.erp.modules.crm.loyalty.domain.LoyaltyConfig;
import com.guru.erp.modules.crm.loyalty.domain.LoyaltyLedger;
import com.guru.erp.modules.crm.loyalty.dto.CustomerTransactionDtos.CustomerTransactionResponse;
import com.guru.erp.modules.crm.loyalty.dto.CustomerTransactionDtos.TransactionLine;
import com.guru.erp.modules.crm.loyalty.dto.CustomerTransactionDtos.TransactionPayment;
import com.guru.erp.modules.crm.loyalty.dto.LoyaltyConfigDtos.EarnRule;
import com.guru.erp.modules.crm.loyalty.dto.LoyaltyConfigDtos.LoyaltyProgramResponse;
import com.guru.erp.modules.crm.loyalty.dto.LoyaltyConfigDtos.LoyaltyTier;
import com.guru.erp.modules.crm.loyalty.dto.LoyaltyConfigDtos.RedemptionRule;
import com.guru.erp.modules.crm.loyalty.dto.LoyaltyLedgerDtos.LedgerEntryResponse;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Entity/JSON-column -> DTO translation for the loyalty slice. Kept as a plain
 * hand-written component (rather than MapStruct) because every conversion here
 * crosses an opaque {@code Map}/{@code List<Map>} JSON column into a typed
 * record — MapStruct has no leverage over that shape, so an explicit mapper
 * reads clearer than annotation expressions.
 */
@Component
public class LoyaltyMapper {

    public LoyaltyProgramResponse toResponse(LoyaltyConfig cfg) {
        return new LoyaltyProgramResponse(
            cfg.getPublicId(),
            cfg.getCompanyId(),
            cfg.getName(),
            cfg.isActive(),
            cfg.getCurrency(),
            toEarnRule(cfg.getEarnRule()),
            toRedemptionRule(cfg.getRedeemRule()),
            cfg.getExpiryMonths(),
            toTiers(cfg.getTiers()),
            cfg.getCreatedAt(),
            cfg.getUpdatedAt(),
            cfg.getVersion());
    }

    public EarnRule toEarnRule(Map<String, Object> raw) {
        double perPoint = toDouble(raw.get("currencyUnitPerPoint"), 1.0);
        @SuppressWarnings("unchecked")
        List<String> categories = raw.get("eligibleCategoryIds") instanceof List<?> l
            ? (List<String>) l : List.of();
        return new EarnRule(perPoint, categories);
    }

    public RedemptionRule toRedemptionRule(Map<String, Object> raw) {
        int perUnit = toInt(raw.get("pointsPerCurrencyUnit"), 100);
        int minBalance = toInt(raw.get("minBalanceForRedemption"), 0);
        return new RedemptionRule(perUnit, minBalance);
    }

    public List<LoyaltyTier> toTiers(List<Map<String, Object>> raw) {
        return raw.stream().map(this::toTier).toList();
    }

    public LoyaltyTier toTier(Map<String, Object> raw) {
        return new LoyaltyTier(
            (String) raw.get("id"),
            (String) raw.get("code"),
            (String) raw.get("name"),
            toLong(raw.get("minSpendAmount"), 0),
            (String) raw.get("currency"),
            toDouble(raw.get("earnMultiplier"), 1.0));
    }

    public Map<String, Object> toMap(LoyaltyTier tier) {
        return Map.of(
            "id", tier.id(),
            "code", tier.code(),
            "name", tier.name(),
            "minSpendAmount", tier.minSpendAmount(),
            "currency", tier.currency(),
            "earnMultiplier", tier.earnMultiplier());
    }

    public LedgerEntryResponse toResponse(LoyaltyLedger ledger) {
        return new LedgerEntryResponse(
            ledger.getPublicId(),
            ledger.getCustomerId(),
            ledger.getType(),
            ledger.getPointsSigned(),
            ledger.getOccurredAt(),
            ledger.getExpiresAt(),
            ledger.getSourceTransactionId(),
            ledger.getDescription());
    }

    public CustomerTransactionResponse toResponse(CustomerTransaction t) {
        return new CustomerTransactionResponse(
            t.getPublicId(),
            t.getCustomerId(),
            t.getReceiptNumber(),
            t.getType(),
            t.getOccurredAt(),
            t.getTotalAmount(),
            t.getSubtotalAmount(),
            t.getTotalCurrency(),
            t.getLocationId(),
            toPayments(t.getPaymentSummary()),
            toLines(t.getLines()),
            t.getRefundOfId());
    }

    private List<TransactionPayment> toPayments(List<Map<String, Object>> raw) {
        return raw.stream()
            .map(m -> new TransactionPayment(
                (String) m.get("method"),
                (String) m.get("maskedPan"),
                toLong(m.get("amount"), 0)))
            .toList();
    }

    private List<TransactionLine> toLines(List<Map<String, Object>> raw) {
        return raw.stream()
            .map(m -> new TransactionLine(
                (String) m.get("sku"),
                (String) m.get("name"),
                toLong(m.get("qty"), 0),
                toLong(m.get("unitPriceAmount"), 0),
                toLong(m.get("lineAmount"), 0),
                (String) m.get("categoryId")))
            .toList();
    }

    private static double toDouble(Object v, double fallback) {
        return v instanceof Number n ? n.doubleValue() : fallback;
    }

    private static int toInt(Object v, int fallback) {
        return v instanceof Number n ? n.intValue() : fallback;
    }

    private static long toLong(Object v, long fallback) {
        return v instanceof Number n ? n.longValue() : fallback;
    }
}

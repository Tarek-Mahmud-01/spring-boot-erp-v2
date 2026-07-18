package com.guru.erp.modules.procurement.returns.service;

import com.guru.erp.modules.procurement.returns.domain.SupplierReturnLine;
import com.guru.erp.modules.procurement.returns.dto.ReturnDtos.ReturnLineRequest;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import com.guru.erp.platform.money.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Prices a set of return lines into a debit-note total, mirroring the reference
 * {@code _compute_return_lines}: for each line the NET value is refunded and line-level GST added on
 * top of the exclusive net. The reference pulls unit price / tax rate from the source PO line via
 * the GRN chain; that chain lives in other slices, so the client supplies the per-line cost inputs
 * on the request (unit price + optional tax rate) and this service does the arithmetic. All rounding
 * is HALF_EVEN, matching the reference {@code ROUND_HALF_EVEN}.
 */
@Service
public class ReturnPricingService {

    static final String DEFAULT_CURRENCY = "USD";

    /** Result of pricing: transaction-currency debit note (net + tax) and the resolved currency. */
    public record Priced(long netTotal, long taxTotal, String currency) {
        public long debitTotal() {
            return netTotal + taxTotal;
        }
    }

    /**
     * Compute the debit-note total for the given lines. The already-returned-versus-received cap is
     * enforced separately in the command service (it needs the repository); here we only value.
     */
    public Priced price(List<ReturnLineRequest> lines) {
        long netTotal = 0;
        long taxTotal = 0;
        String currency = null;
        for (ReturnLineRequest ln : lines) {
            String ccy = ln.unitPriceCurrency() == null || ln.unitPriceCurrency().isBlank()
                ? DEFAULT_CURRENCY : ln.unitPriceCurrency().toUpperCase();
            currency = currency == null ? ccy : currency;
            long unitPrice = ln.unitPriceAmount();
            long netForReturn = BigDecimal.valueOf(unitPrice)
                .multiply(ln.qty())
                .setScale(0, RoundingMode.HALF_EVEN)
                .longValueExact();
            long taxForReturn = 0;
            BigDecimal rate = ln.taxRatePercent();
            if (rate != null && rate.signum() > 0) {
                taxForReturn = BigDecimal.valueOf(netForReturn)
                    .multiply(rate)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_EVEN)
                    .longValueExact();
            }
            netTotal += netForReturn;
            taxTotal += taxForReturn;
        }
        return new Priced(netTotal, taxTotal, currency == null ? DEFAULT_CURRENCY : currency);
    }

    /** Build a persisted line from a request line. */
    public SupplierReturnLine toEntity(ReturnLineRequest in) {
        if (in.qty() == null || in.qty().signum() <= 0) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "qty must be positive");
        }
        SupplierReturnLine line = new SupplierReturnLine();
        line.setGrnLineId(in.grnLineId());
        line.setVariantId(in.variantId());
        line.setQty(in.qty());
        line.setReason(in.reason());
        return line;
    }

    /** The base-currency debit note = debitTotal * rate, HALF_EVEN. */
    public Money baseDebit(long debitTotal, BigDecimal rate, String baseCurrency) {
        long base = BigDecimal.valueOf(debitTotal)
            .multiply(rate)
            .setScale(0, RoundingMode.HALF_EVEN)
            .longValueExact();
        return Money.ofMinor(base, baseCurrency);
    }
}

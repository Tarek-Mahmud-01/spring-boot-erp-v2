package com.springboot.erp.modules.finance.gl.service;

import com.springboot.erp.modules.finance.gl.domain.HolderType;
import com.springboot.erp.modules.finance.gl.domain.JournalEntry;
import com.springboot.erp.modules.finance.gl.domain.JournalLine;
import com.springboot.erp.modules.finance.gl.dto.JournalLineDtos.JournalLineRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * Builds {@link JournalLine} rows from request DTOs and appends them to an entry, converting each
 * line's transaction-currency debit/credit into the company base currency (reference
 * {@code _insert_journal_lines}' per-line FX loop). This slice has no ExchangeRate lookup service
 * of its own (that lives in a not-yet-ported settings/currency-rates concern), so the caller
 * supplies the resolved {@code exchangeRate} directly on the line request — 1 when the line is
 * already in the company's base currency, matching the reference's own {@code rate == 1} fast path
 * exactly (no DB round-trip, base amounts equal the transaction amounts verbatim).
 */
@Component
public class JournalLineFactory {

    private final BalancingService balancing;

    public JournalLineFactory(BalancingService balancing) {
        this.balancing = balancing;
    }

    /** Build and attach one {@link JournalLine} per request, in order, starting at line_no 1. */
    public void appendLines(JournalEntry entry, java.util.List<JournalLineRequest> requests) {
        int lineNo = 1;
        for (JournalLineRequest req : requests) {
            balancing.assertXor(req.debit(), req.credit());

            JournalLine line = new JournalLine();
            line.setLineNo(lineNo++);
            line.setAccountId(req.accountId());
            line.setHolderType(req.holderType() != null
                ? HolderType.valueOf(req.holderType().toUpperCase(java.util.Locale.ROOT))
                : HolderType.NONE);
            line.setHolderId(line.getHolderType() == HolderType.NONE ? null : req.holderId());
            line.setNarration(req.narration() != null ? req.narration() : "");
            line.setDebit(req.debit());
            line.setCredit(req.credit());
            line.setCurrency(req.currency().toUpperCase(java.util.Locale.ROOT));

            BigDecimal rate = req.exchangeRate() != null ? req.exchangeRate() : BigDecimal.ONE;
            line.setExchangeRate(rate);
            line.setBaseDebit(toBase(req.debit(), rate));
            line.setBaseCredit(toBase(req.credit(), rate));
            line.setLocationId(req.locationId() != null ? req.locationId() : entry.getLocationId());

            entry.addLine(line);
        }
    }

    private long toBase(long amountMinor, BigDecimal rate) {
        if (rate.compareTo(BigDecimal.ONE) == 0) {
            return amountMinor;
        }
        return BigDecimal.valueOf(amountMinor)
            .multiply(rate)
            .setScale(0, RoundingMode.HALF_EVEN)
            .longValueExact();
    }
}

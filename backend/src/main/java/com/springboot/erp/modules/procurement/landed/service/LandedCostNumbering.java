package com.springboot.erp.modules.procurement.landed.service;

import com.springboot.erp.modules.procurement.landed.repository.LandedCostRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

/**
 * Mints the per-year landed-cost invoice number ({@code LC-YYYY-NNNN}, reference
 * {@code _next_invoice_number}). Extracted from {@link LandedCostCommandService} to keep that
 * service within the size cap. Zero-padding makes lexical order match numeric order, so scanning the
 * current max suffix is collision-safe.
 */
@Component
public class LandedCostNumbering {

    private final LandedCostRepository repository;
    private final Clock clock = Clock.systemUTC();

    public LandedCostNumbering(LandedCostRepository repository) {
        this.repository = repository;
    }

    public String nextInvoiceNumber() {
        String prefix = "LC-" + Instant.now(clock).atZone(ZoneOffset.UTC).getYear() + "-";
        String last = repository.maxInvoiceNumberForPrefix(prefix);
        int n = 1;
        if (last != null) {
            try {
                n = Integer.parseInt(last.substring(prefix.length()).trim()) + 1;
            } catch (NumberFormatException ignored) {
                n = 1;
            }
        }
        String candidate = prefix + String.format("%04d", n);
        while (repository.existsByInvoiceNumber(candidate)) {
            n++;
            candidate = prefix + String.format("%04d", n);
        }
        return candidate;
    }
}

package com.springboot.erp.modules.finance.gl.service;

import com.springboot.erp.modules.finance.gl.repository.JournalEntryRepository;
import com.springboot.erp.modules.finance.gl.repository.VoucherTypeRepository;
import java.time.LocalDate;
import java.time.Year;
import org.springframework.stereotype.Service;

/**
 * Resolves {@code <PREFIX>-<YYYY>-<NNNNNN>} voucher numbers (reference {@code views/_shared.py}
 * {@code next_voucher_number}). The prefix comes from the {@code VoucherType} catalogue (falling
 * back to the raw voucher-type code when the catalogue has no matching row — mirrors the
 * reference's graceful degradation for a minimal/test environment that hasn't seeded voucher
 * types), and the running counter is scoped to (company, voucherType, calendar year) so every
 * company's V-006 series restarts at 1 each year independently of every other company/type.
 */
@Service
public class VoucherNumberingService {

    private final JournalEntryRepository entryRepository;
    private final VoucherTypeRepository voucherTypeRepository;

    public VoucherNumberingService(JournalEntryRepository entryRepository,
                                   VoucherTypeRepository voucherTypeRepository) {
        this.entryRepository = entryRepository;
        this.voucherTypeRepository = voucherTypeRepository;
    }

    public String next(String companyId, String voucherTypeCode, LocalDate entryDate) {
        String prefix = voucherTypeRepository.findByCode(voucherTypeCode)
            .map(vt -> vt.getPrefix())
            .orElse(voucherTypeCode);
        int year = Year.from(entryDate).getValue();
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        long countThisYear = entryRepository.countByCompanyIdAndVoucherTypeAndEntryDateBetween(
            companyId, voucherTypeCode, yearStart, yearEnd);
        long next = countThisYear + 1;
        // Guard against a rare unique-constraint collision (e.g. an in-flight concurrent create) by
        // walking forward until a free number is found, rather than surfacing a raw DB conflict.
        String candidate;
        do {
            candidate = "%s-%d-%06d".formatted(prefix, year, next);
            next++;
        } while (entryRepository.existsByCompanyIdAndVoucherNumber(companyId, candidate));
        return candidate;
    }
}

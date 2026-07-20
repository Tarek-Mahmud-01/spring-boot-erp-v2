package com.springboot.erp.modules.settings.company.domain;

import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Pure format / checksum validators for ENT-001 Company (ported from the
 * reference {@code validators.py}). No framework dependencies — reused by the
 * service layer for the AC-mapped business errors.
 */
public final class CompanyValidators {

    /** FR-001: company code is 3-10 uppercase alphanumeric. */
    private static final Pattern COMPANY_CODE = Pattern.compile("^[A-Z0-9]{3,10}$");

    /** FR-001 / ENT-001.tax_registration_no — free text, length 5-30. */
    private static final Pattern TAX_REGISTRATION_NO = Pattern.compile("^[A-Za-z0-9 \\-./]{5,30}$");

    /** ENT-001.fiscal_year_start — "MM-DD" calendar pair. */
    private static final Pattern FISCAL_YEAR_START = Pattern.compile("^(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$");

    /** FR-AU-001: ABN position weights for the ATO checksum. */
    private static final int[] ABN_WEIGHTS = {10, 1, 3, 5, 7, 9, 11, 13, 15, 17, 19};

    private CompanyValidators() {
    }

    public static boolean isValidCompanyCode(String code) {
        return code != null && COMPANY_CODE.matcher(code).matches();
    }

    public static boolean isValidTaxRegistrationNo(String value) {
        return value != null && TAX_REGISTRATION_NO.matcher(value).matches();
    }

    /** Strip spaces — ABNs are commonly written {@code 11 222 333 444}. */
    public static String normaliseAbn(String value) {
        return value == null ? "" : value.replace(" ", "");
    }

    /** Strip spaces from an ACN. */
    public static String normaliseAcn(String value) {
        return value == null ? "" : value.replace(" ", "");
    }

    /**
     * ATO ABN checksum (FR-AU-001): subtract 1 from the leftmost digit, weight
     * each digit, and require the weighted sum divisible by 89.
     */
    public static boolean isValidAbn(String value) {
        String abn = normaliseAbn(value);
        if (abn.length() != 11 || !abn.chars().allMatch(Character::isDigit)) {
            return false;
        }
        int total = 0;
        for (int i = 0; i < 11; i++) {
            int digit = abn.charAt(i) - '0';
            if (i == 0) {
                digit -= 1;
            }
            total += digit * ABN_WEIGHTS[i];
        }
        return total % 89 == 0;
    }

    /** FR-AU-002: ACN is 9 digits, no checksum at MVP. */
    public static boolean isValidAcn(String value) {
        String acn = normaliseAcn(value);
        return acn.length() == 9 && acn.chars().allMatch(Character::isDigit);
    }

    /** ENT-001.fiscal_year_start: "MM-DD" for any valid calendar day (leap-safe). */
    public static boolean isValidFiscalYearStart(String value) {
        if (value == null || !FISCAL_YEAR_START.matcher(value).matches()) {
            return false;
        }
        String[] parts = value.split("-");
        int month = Integer.parseInt(parts[0]);
        int day = Integer.parseInt(parts[1]);
        try {
            // 2024 is a leap year so 02-29 is accepted.
            LocalDate.of(2024, month, day);
        } catch (RuntimeException e) {
            return false;
        }
        return true;
    }
}

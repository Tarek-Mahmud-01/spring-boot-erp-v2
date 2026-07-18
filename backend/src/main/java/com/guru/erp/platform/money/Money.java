package com.guru.erp.platform.money;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Money as {@code long} minor units + ISO-4217 {@code char(3)} currency
 * (ARCHITECTURE.md §2). NEVER {@code double}. All rounding is HALF_EVEN.
 *
 * <p>Minor units are the smallest denomination for the currency's default
 * fraction digits (e.g. 1050 + "SAR" = 10.50 SAR). Arithmetic requires the
 * same currency; cross-currency work must convert first via an exchange rate.
 */
@Embeddable
public final class Money implements Serializable, Comparable<Money> {

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency;

    protected Money() {
        // JPA
    }

    private Money(long amountMinor, String currency) {
        this.amountMinor = amountMinor;
        this.currency = Objects.requireNonNull(currency, "currency").toUpperCase();
        if (this.currency.length() != 3) {
            throw new IllegalArgumentException("currency must be ISO-4217 alpha-3: " + currency);
        }
    }

    public static Money ofMinor(long amountMinor, String currency) {
        return new Money(amountMinor, currency);
    }

    /** Zero in the given currency. */
    public static Money zero(String currency) {
        return new Money(0L, currency);
    }

    /**
     * Build from a major-unit decimal (e.g. {@code "10.50"}), rounding to the
     * currency's {@code fractionDigits} with HALF_EVEN.
     */
    public static Money ofMajor(BigDecimal major, String currency, int fractionDigits) {
        long minor = major
            .movePointRight(fractionDigits)
            .setScale(0, RoundingMode.HALF_EVEN)
            .longValueExact();
        return new Money(minor, currency);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(Math.addExact(this.amountMinor, other.amountMinor), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(Math.subtractExact(this.amountMinor, other.amountMinor), currency);
    }

    /** Multiply by a scalar (e.g. quantity), rounding HALF_EVEN back to minor units. */
    public Money multiply(BigDecimal factor) {
        long result = BigDecimal.valueOf(amountMinor)
            .multiply(factor)
            .setScale(0, RoundingMode.HALF_EVEN)
            .longValueExact();
        return new Money(result, currency);
    }

    public boolean isZero() {
        return amountMinor == 0L;
    }

    public boolean isNegative() {
        return amountMinor < 0L;
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "currency mismatch: %s vs %s".formatted(currency, other.currency));
        }
    }

    public long amountMinor() {
        return amountMinor;
    }

    public String currency() {
        return currency;
    }

    @Override
    public int compareTo(Money o) {
        requireSameCurrency(o);
        return Long.compare(amountMinor, o.amountMinor);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money money)) {
            return false;
        }
        return amountMinor == money.amountMinor && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amountMinor, currency);
    }

    @Override
    public String toString() {
        return amountMinor + " " + currency;
    }
}

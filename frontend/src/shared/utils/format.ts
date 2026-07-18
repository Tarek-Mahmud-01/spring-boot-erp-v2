import type { Money } from "@/shared/types/api";

/**
 * Display-only formatters (ARCHITECTURE.md §3 — frontend renders UI only, the
 * backend owns authoritative money math). These never compute business values;
 * they turn already-computed backend values into strings.
 */

/** Default fraction digits per currency; extend as needed. */
const FRACTION_DIGITS: Record<string, number> = {
  SAR: 2,
  USD: 2,
  EUR: 2,
  JPY: 0,
  KWD: 3,
  BHD: 3,
};

function fractionDigitsFor(currency: string): number {
  return FRACTION_DIGITS[currency.toUpperCase()] ?? 2;
}

/** Format backend Money (minor units) for display, e.g. 105000 SAR → "1,050.00". */
export function formatMoney(money: Money, locale = "en"): string {
  const digits = fractionDigitsFor(money.currency);
  const major = money.amountMinor / 10 ** digits;
  return new Intl.NumberFormat(locale, {
    style: "currency",
    currency: money.currency,
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  }).format(major);
}

/** Format an ISO/epoch date for display. */
export function formatDate(value: string | number | Date, locale = "en"): string {
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return new Intl.DateTimeFormat(locale, {
    year: "numeric",
    month: "short",
    day: "2-digit",
  }).format(date);
}

/** Format an ISO/epoch datetime for display. */
export function formatDateTime(value: string | number | Date, locale = "en"): string {
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return new Intl.DateTimeFormat(locale, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

/**
 * Live-preview line + document totals (ARCHITECTURE.md §3 — display only; the
 * backend recomputes authoritative totals on save). All amounts are integer
 * MINOR units; quantities/percentages are plain numbers.
 */
export interface LineTotals {
  gross: number;
  discountAmount: number;
  subtotal: number;
  taxAmount: number;
  total: number;
}

const ZERO: LineTotals = { gross: 0, discountAmount: 0, subtotal: 0, taxAmount: 0, total: 0 };

/** One line: gross → line discount → subtotal → tax → total (minor units). */
export function calcLine(
  qty: number,
  unitPriceMinor: number,
  discountPercent = 0,
  taxRatePercent = 0,
): LineTotals {
  const disc = Math.min(100, Math.max(0, discountPercent || 0));
  const gross = Math.round((qty || 0) * (unitPriceMinor || 0));
  const discountAmount = Math.round((gross * disc) / 100);
  const subtotal = gross - discountAmount;
  const taxAmount = Math.round((subtotal * (taxRatePercent || 0)) / 100);
  return { gross, discountAmount, subtotal, taxAmount, total: subtotal + taxAmount };
}

/** Sum many line totals into a document total. */
export function calcDocumentTotals(lines: LineTotals[]): LineTotals {
  return lines.reduce(
    (a, l) => ({
      gross: a.gross + l.gross,
      discountAmount: a.discountAmount + l.discountAmount,
      subtotal: a.subtotal + l.subtotal,
      taxAmount: a.taxAmount + l.taxAmount,
      total: a.total + l.total,
    }),
    { ...ZERO },
  );
}

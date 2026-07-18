import { describe, expect, it } from "vitest";
import { formatMoney } from "./format";

describe("formatMoney", () => {
  it("formats SAR minor units to 2 decimals", () => {
    // 105000 minor = 1,050.00 SAR
    const out = formatMoney({ amountMinor: 105000, currency: "SAR" });
    expect(out).toMatch(/1,050\.00/);
  });

  it("respects zero-decimal currencies (JPY)", () => {
    const out = formatMoney({ amountMinor: 1050, currency: "JPY" });
    expect(out).toMatch(/1,050/);
    expect(out).not.toMatch(/\./);
  });

  it("respects three-decimal currencies (KWD)", () => {
    const out = formatMoney({ amountMinor: 1050, currency: "KWD" });
    // 1050 minor with 3 digits = 1.050
    expect(out).toMatch(/1\.050/);
  });
});

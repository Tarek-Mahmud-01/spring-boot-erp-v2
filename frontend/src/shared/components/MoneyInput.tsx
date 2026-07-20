import { forwardRef, useEffect, useState, type InputHTMLAttributes } from "react";
import { cn } from "@/shared/utils/cn";

export interface MoneyInputProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, "value" | "onChange" | "type"> {
  /** Amount in MINOR units (e.g. cents). */
  value: number;
  /** Emits MINOR units. */
  onChange: (minor: number) => void;
  /** ISO currency code shown as a prefix (e.g. "SAR"). */
  currency?: string;
  /** Minor-unit digits (default 2). */
  decimals?: number;
  invalid?: boolean;
}

const factor = (decimals: number) => 10 ** decimals;

/** Currency-aware money input: displays major units, emits minor units. */
export const MoneyInput = forwardRef<HTMLInputElement, MoneyInputProps>(function MoneyInput(
  { value, onChange, currency, decimals = 2, invalid, disabled, className, ...rest },
  ref,
) {
  const [text, setText] = useState(() => (value / factor(decimals)).toFixed(decimals));

  useEffect(() => {
    const major = value / factor(decimals);
    // Only resync when the numeric value diverges from what's typed, so the
    // user can keep typing "12." without it being reformatted mid-entry.
    if (Number.parseFloat(text || "0") !== major) setText(major ? String(major) : "");
  }, [value, decimals]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="relative">
      {currency && (
        <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-small font-medium text-fg-muted">
          {currency}
        </span>
      )}
      <input
        ref={ref}
        {...rest}
        type="number"
        inputMode="decimal"
        step={1 / factor(decimals)}
        value={text}
        disabled={disabled}
        aria-invalid={invalid || undefined}
        onChange={(e) => {
          setText(e.target.value);
          const n = Number.parseFloat(e.target.value);
          onChange(Number.isFinite(n) ? Math.round(n * factor(decimals)) : 0);
        }}
        className={cn(
          "h-9 w-full rounded-md border bg-surface px-3 text-right text-body tabular-nums text-fg",
          "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-0 focus-visible:outline-primary",
          "disabled:cursor-not-allowed disabled:opacity-60",
          currency ? "pl-12" : "",
          invalid ? "border-danger" : "border-border",
          className,
        )}
      />
    </div>
  );
});

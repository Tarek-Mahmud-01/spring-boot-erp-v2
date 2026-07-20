import { forwardRef, type InputHTMLAttributes } from "react";
import { cn } from "@/shared/utils/cn";

export interface QuantityInputProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, "value" | "onChange" | "type"> {
  value: number;
  onChange: (value: number) => void;
  /** Decimal places allowed (default 3). */
  decimals?: number;
  invalid?: boolean;
}

/** Decimal quantity input. Emits a number. */
export const QuantityInput = forwardRef<HTMLInputElement, QuantityInputProps>(function QuantityInput(
  { value, onChange, decimals = 3, invalid, disabled, className, ...rest },
  ref,
) {
  return (
    <input
      ref={ref}
      {...rest}
      type="number"
      inputMode="decimal"
      step={1 / 10 ** decimals}
      value={Number.isFinite(value) ? value : 0}
      disabled={disabled}
      aria-invalid={invalid || undefined}
      onChange={(e) => {
        const n = Number.parseFloat(e.target.value);
        onChange(Number.isFinite(n) ? n : 0);
      }}
      className={cn(
        "h-9 w-full rounded-md border bg-surface px-3 text-right text-body tabular-nums text-fg",
        "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-0 focus-visible:outline-primary",
        "disabled:cursor-not-allowed disabled:opacity-60",
        invalid ? "border-danger" : "border-border",
        className,
      )}
    />
  );
});

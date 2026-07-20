import { forwardRef, type InputHTMLAttributes } from "react";
import { cn } from "@/shared/utils/cn";

export interface PercentInputProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, "value" | "onChange" | "type"> {
  value: number;
  onChange: (value: number) => void;
  invalid?: boolean;
}

/** Percentage input (0–100) with a trailing % adornment. Emits a number. */
export const PercentInput = forwardRef<HTMLInputElement, PercentInputProps>(function PercentInput(
  { value, onChange, invalid, disabled, className, ...rest },
  ref,
) {
  return (
    <div className="relative">
      <input
        ref={ref}
        {...rest}
        type="number"
        inputMode="decimal"
        min={0}
        max={100}
        step={0.01}
        value={Number.isFinite(value) ? value : 0}
        disabled={disabled}
        aria-invalid={invalid || undefined}
        onChange={(e) => {
          const n = Number.parseFloat(e.target.value);
          onChange(Number.isFinite(n) ? n : 0);
        }}
        className={cn(
          "h-9 w-full rounded-md border bg-surface pl-3 pr-8 text-right text-body tabular-nums text-fg",
          "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-0 focus-visible:outline-primary",
          "disabled:cursor-not-allowed disabled:opacity-60",
          invalid ? "border-danger" : "border-border",
          className,
        )}
      />
      <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-small font-medium text-fg-muted">
        %
      </span>
    </div>
  );
});

import { forwardRef, type InputHTMLAttributes } from "react";
import { Check } from "lucide-react";
import { cn } from "@/shared/utils/cn";

interface CheckboxProps extends Omit<InputHTMLAttributes<HTMLInputElement>, "type"> {
  label?: string;
}

/** Token-styled checkbox with an optional inline label. */
export const Checkbox = forwardRef<HTMLInputElement, CheckboxProps>(function Checkbox(
  { label, className, checked, disabled, ...props },
  ref,
) {
  return (
    <label className={cn("inline-flex cursor-pointer items-center gap-2", disabled && "cursor-not-allowed opacity-60", className)}>
      <span className="relative inline-flex h-4 w-4 items-center justify-center">
        <input
          ref={ref}
          type="checkbox"
          checked={checked}
          disabled={disabled}
          className="peer sr-only"
          {...props}
        />
        <span
          className={cn(
            "inline-flex h-4 w-4 items-center justify-center rounded border border-border bg-surface transition-colors",
            "peer-checked:border-primary peer-checked:bg-primary",
            "peer-focus-visible:outline peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-primary",
          )}
        >
          {checked && <Check className="h-3 w-3 text-neutral-0" aria-hidden />}
        </span>
      </span>
      {label && <span className="text-body text-fg">{label}</span>}
    </label>
  );
});

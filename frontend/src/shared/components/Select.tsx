import { forwardRef, type SelectHTMLAttributes } from "react";
import { cn } from "@/shared/utils/cn";

interface Option {
  value: string;
  label: string;
}

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  options: Option[];
  invalid?: boolean;
  placeholder?: string;
}

/** Native select, token-styled (ARCHITECTURE.md §3 shared/components). */
export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select(
  { options, invalid, placeholder, className, ...props },
  ref,
) {
  return (
    <select
      ref={ref}
      aria-invalid={invalid || undefined}
      className={cn(
        "h-9 w-full rounded-md border bg-surface px-3 text-body text-fg",
        "focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary",
        "disabled:cursor-not-allowed disabled:opacity-60",
        invalid ? "border-danger" : "border-border",
        className,
      )}
      {...props}
    >
      {placeholder && <option value="">{placeholder}</option>}
      {options.map((o) => (
        <option key={o.value} value={o.value}>
          {o.label}
        </option>
      ))}
    </select>
  );
});

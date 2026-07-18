import { type ReactNode, useId } from "react";
import { cn } from "@/shared/utils/cn";

interface FieldProps {
  label: string;
  htmlFor?: string;
  error?: string;
  required?: boolean;
  hint?: string;
  className?: string;
  children: (props: { id: string; invalid: boolean }) => ReactNode;
}

/**
 * Form field wrapper: label + control + error/hint (ARCHITECTURE.md §3). Pairs
 * with RHF + zod. The render-prop hands the control a stable id + invalid flag
 * so labels associate and aria-invalid is set consistently.
 */
export function Field({ label, error, required, hint, className, children }: FieldProps) {
  const id = useId();
  const invalid = Boolean(error);
  return (
    <div className={cn("flex flex-col gap-1.5", className)}>
      <label htmlFor={id} className="text-small font-semibold text-fg">
        {label}
        {required && <span className="ml-0.5 text-danger">*</span>}
      </label>
      {children({ id, invalid })}
      {error ? (
        <p className="text-small text-danger">{error}</p>
      ) : hint ? (
        <p className="text-small text-fg-muted">{hint}</p>
      ) : null}
    </div>
  );
}

import { cn } from "@/shared/utils/cn";

interface RadioOption {
  value: string;
  label: string;
  hint?: string;
}

interface RadioGroupProps {
  name: string;
  options: RadioOption[];
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  className?: string;
}

/** Vertical radio group with token-styled indicators. */
export function RadioGroup({ name, options, value, onChange, disabled, className }: RadioGroupProps) {
  return (
    <div role="radiogroup" className={cn("flex flex-col gap-2", className)}>
      {options.map((o) => {
        const selected = o.value === value;
        return (
          <label
            key={o.value}
            className={cn(
              "flex cursor-pointer items-start gap-2.5 rounded-md border border-border p-2.5 transition-colors",
              selected ? "border-primary bg-surface-muted" : "hover:bg-surface-muted",
              disabled && "cursor-not-allowed opacity-60",
            )}
          >
            <input
              type="radio"
              name={name}
              value={o.value}
              checked={selected}
              disabled={disabled}
              onChange={() => onChange(o.value)}
              className="peer sr-only"
            />
            <span
              className={cn(
                "mt-0.5 inline-flex h-4 w-4 shrink-0 items-center justify-center rounded-full border border-border bg-surface",
                selected && "border-primary",
              )}
            >
              {selected && <span className="h-2 w-2 rounded-full bg-primary" aria-hidden />}
            </span>
            <span className="flex flex-col">
              <span className="text-body text-fg">{o.label}</span>
              {o.hint && <span className="text-small text-fg-muted">{o.hint}</span>}
            </span>
          </label>
        );
      })}
    </div>
  );
}

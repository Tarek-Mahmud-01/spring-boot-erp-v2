import { useEffect, useRef, useState } from "react";
import { Check, ChevronDown, X } from "lucide-react";
import { cn } from "@/shared/utils/cn";

interface Option {
  value: string;
  label: string;
}

interface MultiSelectProps {
  options: Option[];
  value: string[];
  onChange: (value: string[]) => void;
  placeholder?: string;
  disabled?: boolean;
  invalid?: boolean;
}

/** Chip-based multi-select with a checklist dropdown (client-side options). */
export function MultiSelect({ options, value, onChange, placeholder = "Select…", disabled, invalid }: MultiSelectProps) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const onDown = (e: MouseEvent) => {
      if (!rootRef.current?.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", onDown);
    return () => document.removeEventListener("mousedown", onDown);
  }, [open]);

  const toggle = (v: string) =>
    onChange(value.includes(v) ? value.filter((x) => x !== v) : [...value, v]);
  const selected = options.filter((o) => value.includes(o.value));

  return (
    <div ref={rootRef} className="relative">
      <button
        type="button"
        disabled={disabled}
        aria-invalid={invalid || undefined}
        aria-expanded={open}
        onClick={() => !disabled && setOpen((o) => !o)}
        className={cn(
          "flex min-h-9 w-full items-center justify-between gap-2 rounded-md border bg-surface px-2 py-1 text-left",
          "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-0 focus-visible:outline-primary",
          "disabled:cursor-not-allowed disabled:opacity-60",
          invalid ? "border-danger" : "border-border",
        )}
      >
        <span className="flex flex-1 flex-wrap gap-1">
          {selected.length === 0 ? (
            <span className="px-1 text-body text-fg-muted">{placeholder}</span>
          ) : (
            selected.map((o) => (
              <span
                key={o.value}
                className="inline-flex items-center gap-1 rounded-md border border-border bg-surface-muted px-2 py-0.5 text-small text-fg"
              >
                {o.label}
                <span
                  role="button"
                  tabIndex={-1}
                  aria-label={`Remove ${o.label}`}
                  onClick={(e) => {
                    e.stopPropagation();
                    toggle(o.value);
                  }}
                  className="text-fg-muted hover:text-fg"
                >
                  <X className="h-3 w-3" />
                </span>
              </span>
            ))
          )}
        </span>
        <ChevronDown className="h-4 w-4 shrink-0 text-fg-muted" aria-hidden />
      </button>
      {open && (
        <div className="absolute z-50 mt-1 max-h-60 w-full overflow-auto rounded-md border border-border bg-surface py-1 shadow-lg">
          {options.map((o) => {
            const on = value.includes(o.value);
            return (
              <button
                key={o.value}
                type="button"
                onClick={() => toggle(o.value)}
                className="flex w-full items-center justify-between px-3 py-1.5 text-left text-body text-fg hover:bg-surface-muted"
              >
                {o.label}
                {on && <Check className="h-4 w-4 text-fg" aria-hidden />}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}

import {
  useEffect,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
} from "react";
import { createPortal } from "react-dom";
import { ChevronDown, Loader2, Search, X } from "lucide-react";
import { cn } from "@/shared/utils/cn";

export interface EntityOption {
  value: string;
  label: string;
  hint?: string;
  meta?: unknown;
}

interface EntitySelectProps {
  value: string;
  onChange: (value: string, option: EntityOption | null) => void;
  /** Debounced async search. Returns options for the typed query ("" on open). */
  fetchOptions: (query: string) => Promise<EntityOption[]>;
  /** Label to show for the current `value` when its option isn't in the last result set. */
  selectedLabel?: string;
  placeholder?: string;
  disabled?: boolean;
  invalid?: boolean;
  clearable?: boolean;
  debounceMs?: number;
}

/** Async, type-to-search entity picker (combobox). Portaled dropdown so it works inside modals/drawers. */
export function EntitySelect({
  value,
  onChange,
  fetchOptions,
  selectedLabel,
  placeholder = "Search…",
  disabled,
  invalid,
  clearable = true,
  debounceMs = 300,
}: EntitySelectProps) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [options, setOptions] = useState<EntityOption[]>([]);
  const [loading, setLoading] = useState(false);
  const [style, setStyle] = useState<CSSProperties>({});
  const [label, setLabel] = useState(selectedLabel ?? "");
  const rootRef = useRef<HTMLDivElement>(null);
  const reqId = useRef(0);

  useEffect(() => setLabel(selectedLabel ?? ""), [selectedLabel]);

  // Debounced fetch whenever the dropdown is open and the query changes.
  useEffect(() => {
    if (!open) return;
    const id = ++reqId.current;
    setLoading(true);
    const t = setTimeout(() => {
      fetchOptions(query)
        .then((opts) => {
          if (id === reqId.current) setOptions(opts);
        })
        .catch(() => {
          if (id === reqId.current) setOptions([]);
        })
        .finally(() => {
          if (id === reqId.current) setLoading(false);
        });
    }, debounceMs);
    return () => clearTimeout(t);
  }, [open, query, fetchOptions, debounceMs]);

  // Position + outside-click/escape handling while open.
  useEffect(() => {
    if (!open) return;
    const place = () => {
      const rect = rootRef.current?.getBoundingClientRect();
      if (rect) setStyle({ position: "fixed", top: rect.bottom + 4, left: rect.left, width: rect.width });
    };
    place();
    const onDown = (e: MouseEvent) => {
      const r = rootRef.current;
      const inRoot = r?.contains(e.target as Node);
      const inPop = (e.target as HTMLElement)?.closest?.("[data-entity-pop]");
      if (!inRoot && !inPop) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && setOpen(false);
    window.addEventListener("scroll", place, true);
    window.addEventListener("resize", place);
    document.addEventListener("mousedown", onDown);
    document.addEventListener("keydown", onKey);
    return () => {
      window.removeEventListener("scroll", place, true);
      window.removeEventListener("resize", place);
      document.removeEventListener("mousedown", onDown);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  const display = useMemo(() => label || (value ? value : ""), [label, value]);

  return (
    <div ref={rootRef} className="relative">
      <button
        type="button"
        disabled={disabled}
        aria-invalid={invalid || undefined}
        aria-expanded={open}
        onClick={() => {
          if (disabled) return;
          setQuery("");
          setOpen((o) => !o);
        }}
        className={cn(
          "flex h-9 w-full items-center justify-between gap-2 rounded-md border bg-surface px-3 text-left text-body",
          "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-0 focus-visible:outline-primary",
          "disabled:cursor-not-allowed disabled:opacity-60",
          invalid ? "border-danger" : "border-border",
        )}
      >
        <span className={cn("truncate", display ? "text-fg" : "text-fg-muted")}>{display || placeholder}</span>
        <span className="flex shrink-0 items-center gap-1">
          {clearable && value && !disabled && (
            <span
              role="button"
              tabIndex={-1}
              aria-label="Clear"
              onClick={(e) => {
                e.stopPropagation();
                setLabel("");
                onChange("", null);
              }}
              className="text-fg-muted hover:text-fg"
            >
              <X className="h-4 w-4" />
            </span>
          )}
          <ChevronDown className="h-4 w-4 text-fg-muted" aria-hidden />
        </span>
      </button>

      {open &&
        createPortal(
          <div
            data-entity-pop
            style={style}
            className="z-[200] overflow-hidden rounded-md border border-border bg-surface shadow-lg"
          >
            <div className="flex items-center gap-2 border-b border-border px-3 py-2">
              <Search className="h-4 w-4 shrink-0 text-fg-muted" aria-hidden />
              <input
                autoFocus
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Type to search…"
                className="w-full bg-transparent text-body text-fg placeholder:text-fg-muted focus:outline-none"
              />
              {loading && <Loader2 className="h-4 w-4 shrink-0 animate-spin text-fg-muted" aria-hidden />}
            </div>
            <div className="max-h-64 overflow-auto py-1">
              {!loading && options.length === 0 ? (
                <div className="px-3 py-6 text-center text-small text-fg-muted">No matches</div>
              ) : (
                options.map((o) => (
                  <button
                    key={o.value}
                    type="button"
                    onClick={() => {
                      setLabel(o.label);
                      onChange(o.value, o);
                      setOpen(false);
                    }}
                    className={cn(
                      "flex w-full flex-col items-start px-3 py-1.5 text-left hover:bg-surface-muted",
                      o.value === value && "bg-surface-muted",
                    )}
                  >
                    <span className="text-body text-fg">{o.label}</span>
                    {o.hint && <span className="text-small text-fg-muted">{o.hint}</span>}
                  </button>
                ))
              )}
            </div>
          </div>,
          document.body,
        )}
    </div>
  );
}

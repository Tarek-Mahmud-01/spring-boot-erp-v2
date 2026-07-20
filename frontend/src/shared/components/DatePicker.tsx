import {
  forwardRef,
  useEffect,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type InputHTMLAttributes,
} from "react";
import { createPortal } from "react-dom";
import { Calendar as CalIcon, ChevronLeft, ChevronRight } from "lucide-react";
import { cn } from "@/shared/utils/cn";
import { Input } from "./Input";

export interface DatePickerProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, "type" | "value" | "onChange" | "defaultValue"> {
  /** ISO "yyyy-mm-dd" or "". */
  value: string;
  /** Emits ISO "yyyy-mm-dd" or "" when cleared. */
  onChange: (iso: string) => void;
  min?: string;
  max?: string;
  invalid?: boolean;
}

// --- ISO <-> dd/mm/yyyy helpers ---
function isoToDDMMYYYY(iso: string): string {
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso);
  return m ? `${m[3]}/${m[2]}/${m[1]}` : "";
}
function ddmmyyyyToISO(s: string): string {
  const m = /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/.exec(s.trim());
  if (!m) return "";
  const iso = `${m[3]}-${m[2].padStart(2, "0")}-${m[1].padStart(2, "0")}`;
  const d = new Date(`${iso}T00:00:00Z`);
  if (Number.isNaN(d.getTime()) || d.toISOString().slice(0, 10) !== iso) return "";
  return iso;
}
function autoFormat(raw: string): string {
  const digits = raw.replace(/\D/g, "").slice(0, 8);
  if (digits.length <= 2) return digits;
  if (digits.length <= 4) return `${digits.slice(0, 2)}/${digits.slice(2)}`;
  return `${digits.slice(0, 2)}/${digits.slice(2, 4)}/${digits.slice(4)}`;
}

// --- calendar helpers (UTC-safe) ---
const pad2 = (n: number) => String(n).padStart(2, "0");
const toISODate = (y: number, mi: number, d: number) => `${y}-${pad2(mi + 1)}-${pad2(d)}`;
function todayISO(): string {
  const d = new Date();
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
}
const daysInMonth = (y: number, mi: number) => new Date(Date.UTC(y, mi + 1, 0)).getUTCDate();
const firstDayWeekday = (y: number, mi: number) => new Date(Date.UTC(y, mi, 1)).getUTCDay();

const MONTHS = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December",
];
const WEEKDAYS = ["Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"];

interface PopoverProps {
  selectedISO: string;
  minISO?: string;
  maxISO?: string;
  onSelect: (iso: string) => void;
  onClear: () => void;
  onClose: () => void;
  style: CSSProperties;
}

function CalendarPopover({ selectedISO, minISO, maxISO, onSelect, onClear, onClose, style }: PopoverProps) {
  const today = todayISO();
  const initial = useMemo(() => {
    const [y, m] = (selectedISO || today).split("-").map(Number);
    return { year: y, monthIdx: m - 1 };
  }, [selectedISO, today]);
  const [view, setView] = useState(initial);
  useEffect(() => setView(initial), [initial]);

  const thisYear = Number(today.slice(0, 4));
  const years = useMemo(() => {
    const out: number[] = [];
    for (let y = thisYear + 15; y >= 1950; y--) out.push(y);
    return out;
  }, [thisYear]);

  const goPrev = () =>
    setView((v) => (v.monthIdx === 0 ? { year: v.year - 1, monthIdx: 11 } : { year: v.year, monthIdx: v.monthIdx - 1 }));
  const goNext = () =>
    setView((v) => (v.monthIdx === 11 ? { year: v.year + 1, monthIdx: 0 } : { year: v.year, monthIdx: v.monthIdx + 1 }));

  const cells = useMemo(() => {
    const { year, monthIdx } = view;
    const lead = firstDayWeekday(year, monthIdx);
    const total = daysInMonth(year, monthIdx);
    const prevTotal = daysInMonth(monthIdx === 0 ? year - 1 : year, monthIdx === 0 ? 11 : monthIdx - 1);
    const out: { iso: string; day: number; inMonth: boolean }[] = [];
    for (let i = lead - 1; i >= 0; i--) {
      const py = monthIdx === 0 ? year - 1 : year;
      const pm = monthIdx === 0 ? 11 : monthIdx - 1;
      out.push({ iso: toISODate(py, pm, prevTotal - i), day: prevTotal - i, inMonth: false });
    }
    for (let d = 1; d <= total; d++) out.push({ iso: toISODate(year, monthIdx, d), day: d, inMonth: true });
    let d = 1;
    while (out.length < 42) {
      const ny = monthIdx === 11 ? year + 1 : year;
      const nm = monthIdx === 11 ? 0 : monthIdx + 1;
      out.push({ iso: toISODate(ny, nm, d), day: d, inMonth: false });
      d++;
    }
    return out;
  }, [view]);

  const oob = (iso: string) => Boolean((minISO && iso < minISO) || (maxISO && iso > maxISO));
  const selCls =
    "h-7 rounded-md border border-border bg-surface px-1.5 text-small font-semibold text-fg focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary";

  return (
    <div
      role="dialog"
      aria-label="Choose a date"
      className="z-[200] w-[280px] rounded-md border border-border bg-surface p-3 shadow-lg"
      style={style}
      onMouseDown={(e) => e.preventDefault()}
    >
      <div className="mb-2 flex items-center justify-between gap-2">
        <div className="flex items-center gap-1.5">
          <select
            aria-label="Month"
            value={view.monthIdx}
            onChange={(e) => setView((v) => ({ ...v, monthIdx: Number(e.target.value) }))}
            className={selCls}
          >
            {MONTHS.map((m, i) => (
              <option key={m} value={i}>
                {m}
              </option>
            ))}
          </select>
          <select
            aria-label="Year"
            value={view.year}
            onChange={(e) => setView((v) => ({ ...v, year: Number(e.target.value) }))}
            className={cn(selCls, "tabular-nums")}
          >
            {years.map((y) => (
              <option key={y} value={y}>
                {y}
              </option>
            ))}
          </select>
        </div>
        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={goPrev}
            aria-label="Previous month"
            className="inline-flex h-7 w-7 items-center justify-center rounded-md text-fg-muted hover:bg-surface-muted hover:text-fg"
          >
            <ChevronLeft className="h-4 w-4" aria-hidden />
          </button>
          <button
            type="button"
            onClick={goNext}
            aria-label="Next month"
            className="inline-flex h-7 w-7 items-center justify-center rounded-md text-fg-muted hover:bg-surface-muted hover:text-fg"
          >
            <ChevronRight className="h-4 w-4" aria-hidden />
          </button>
        </div>
      </div>

      <div className="mb-1 grid grid-cols-7 text-center text-[11px] font-medium uppercase tracking-wider text-fg-muted">
        {WEEKDAYS.map((d) => (
          <div key={d} className="py-1">
            {d}
          </div>
        ))}
      </div>

      <div className="grid grid-cols-7 gap-0.5">
        {cells.map((c) => {
          const isSelected = c.iso === selectedISO;
          const isToday = c.iso === today;
          const disabled = oob(c.iso);
          return (
            <button
              key={c.iso}
              type="button"
              disabled={disabled}
              onClick={() => {
                onSelect(c.iso);
                onClose();
              }}
              aria-label={c.iso}
              aria-pressed={isSelected || undefined}
              className={cn(
                "inline-flex h-8 w-8 items-center justify-center rounded-md text-small tabular-nums transition-colors",
                "focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary",
                c.inMonth ? "text-fg" : "text-fg-muted opacity-50",
                !isSelected && !disabled && "hover:bg-surface-muted",
                isSelected && "bg-primary font-semibold text-neutral-0 hover:bg-primary-700",
                isToday && !isSelected && "ring-1 ring-inset ring-border",
                disabled && "cursor-not-allowed opacity-40",
              )}
            >
              {c.day}
            </button>
          );
        })}
      </div>

      <div className="mt-2 flex items-center justify-between border-t border-border pt-2">
        <button
          type="button"
          onClick={() => {
            onClear();
            onClose();
          }}
          className="text-small font-medium text-fg-muted hover:text-danger"
        >
          Clear
        </button>
        <button
          type="button"
          onClick={() => {
            onSelect(today);
            onClose();
          }}
          disabled={oob(today)}
          className="text-small font-semibold text-fg hover:text-fg-muted disabled:cursor-not-allowed disabled:opacity-40"
        >
          Today
        </button>
      </div>
    </div>
  );
}

/** Custom dd/mm/yyyy date picker with a themed calendar popover (matches the design reference). */
export const DatePicker = forwardRef<HTMLInputElement, DatePickerProps>(function DatePicker(
  { value, onChange, placeholder = "dd/mm/yyyy", min, max, className, disabled, onBlur, ...rest },
  ref,
) {
  const [text, setText] = useState(() => isoToDDMMYYYY(value));
  const [open, setOpen] = useState(false);
  const [style, setStyle] = useState<CSSProperties>({});
  const rootRef = useRef<HTMLDivElement>(null);

  useEffect(() => setText(isoToDDMMYYYY(value)), [value]);

  useEffect(() => {
    if (!open) return;
    const place = () => {
      const rect = rootRef.current?.getBoundingClientRect();
      if (rect) setStyle({ position: "fixed", top: rect.bottom + 4, left: rect.left });
    };
    place();
    window.addEventListener("scroll", place, true);
    window.addEventListener("resize", place);
    return () => {
      window.removeEventListener("scroll", place, true);
      window.removeEventListener("resize", place);
    };
  }, [open]);

  useEffect(() => {
    if (!open) return;
    const onDown = (e: MouseEvent) => {
      if (!rootRef.current?.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && setOpen(false);
    document.addEventListener("mousedown", onDown);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDown);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  return (
    <div ref={rootRef} className="relative">
      <Input
        ref={ref}
        {...rest}
        type="text"
        inputMode="numeric"
        autoComplete="off"
        value={text}
        placeholder={placeholder}
        disabled={disabled}
        className={cn("pr-8 tabular-nums", className)}
        onChange={(e) => {
          const f = autoFormat(e.target.value);
          setText(f);
          if (f === "") onChange("");
          else {
            const iso = ddmmyyyyToISO(f);
            if (iso) onChange(iso);
          }
        }}
        onClick={() => !disabled && setOpen((o) => !o)}
        onBlur={(e) => {
          if (text === "") {
            if (value !== "") onChange("");
          } else {
            const iso = ddmmyyyyToISO(text);
            if (iso) {
              setText(isoToDDMMYYYY(iso));
              if (iso !== value) onChange(iso);
            } else setText(isoToDDMMYYYY(value));
          }
          onBlur?.(e);
        }}
      />
      <button
        type="button"
        tabIndex={-1}
        onClick={() => !disabled && setOpen((o) => !o)}
        disabled={disabled}
        aria-label="Open calendar"
        className={cn(
          "absolute right-1 top-1/2 inline-flex h-6 w-6 -translate-y-1/2 items-center justify-center rounded-md",
          "text-fg-muted transition-colors hover:bg-surface-muted hover:text-fg disabled:opacity-50",
          open && "bg-surface-muted text-fg",
        )}
      >
        <CalIcon className="h-4 w-4" aria-hidden />
      </button>
      {open
        ? createPortal(
            <CalendarPopover
              selectedISO={value}
              minISO={min}
              maxISO={max}
              onSelect={onChange}
              onClear={() => onChange("")}
              onClose={() => setOpen(false)}
              style={style}
            />,
            document.body,
          )
        : null}
    </div>
  );
});

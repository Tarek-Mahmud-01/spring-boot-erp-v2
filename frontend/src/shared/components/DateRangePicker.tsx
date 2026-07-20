import { DatePicker } from "./DatePicker";

export interface DateRangeValue {
  from: string;
  to: string;
}

interface DateRangePickerProps {
  value: DateRangeValue;
  onChange: (value: DateRangeValue) => void;
  disabled?: boolean;
}

/** From/To pair built from two DatePickers; `to` is bounded below by `from`. */
export function DateRangePicker({ value, onChange, disabled }: DateRangePickerProps) {
  return (
    <div className="flex items-center gap-2">
      <DatePicker
        aria-label="From"
        value={value.from}
        max={value.to || undefined}
        disabled={disabled}
        onChange={(from) => onChange({ ...value, from })}
      />
      <span className="text-small text-fg-muted">–</span>
      <DatePicker
        aria-label="To"
        value={value.to}
        min={value.from || undefined}
        disabled={disabled}
        onChange={(to) => onChange({ ...value, to })}
      />
    </div>
  );
}

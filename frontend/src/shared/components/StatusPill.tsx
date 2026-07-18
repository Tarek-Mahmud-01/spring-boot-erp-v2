import { cn } from "@/shared/utils/cn";

type Tone = "success" | "warning" | "danger" | "info" | "neutral";

interface StatusPillProps {
  label: string;
  tone?: Tone;
  className?: string;
}

const TONES: Record<Tone, { dot: string; text: string }> = {
  success: { dot: "bg-success", text: "text-success-700" },
  warning: { dot: "bg-warning", text: "text-warning-700" },
  danger: { dot: "bg-danger", text: "text-danger-700" },
  info: { dot: "bg-info", text: "text-info-700" },
  neutral: { dot: "bg-neutral-400", text: "text-fg-muted" },
};

/**
 * Status pill (ARCHITECTURE.md §3). A small tone dot + label — semantic status
 * color, distinct from the brand accent. Maps a domain status to a tone via a
 * lookup at the call site.
 */
export function StatusPill({ label, tone = "neutral", className }: StatusPillProps) {
  const t = TONES[tone];
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full border border-border bg-surface px-2.5 py-0.5 text-small font-medium",
        t.text,
        className,
      )}
    >
      <span className={cn("h-1.5 w-1.5 rounded-full", t.dot)} aria-hidden />
      {label}
    </span>
  );
}

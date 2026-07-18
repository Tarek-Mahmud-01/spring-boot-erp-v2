import { useEffect } from "react";
import { CheckCircle2, XCircle, Info, X } from "lucide-react";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { dismissToast, type Toast } from "@/app/store/toastSlice";
import { cn } from "@/shared/utils/cn";

const ICONS = { success: CheckCircle2, error: XCircle, info: Info };
const TONES = {
  success: "border-success text-success-700",
  error: "border-danger text-danger-700",
  info: "border-info text-info-700",
};

function ToastItem({ toast }: { toast: Toast }) {
  const dispatch = useAppDispatch();
  const Icon = ICONS[toast.tone];

  useEffect(() => {
    const t = setTimeout(() => dispatch(dismissToast(toast.id)), 4000);
    return () => clearTimeout(t);
  }, [dispatch, toast.id]);

  return (
    <div
      role="status"
      className={cn(
        "flex items-start gap-2 rounded-md border-l-4 border border-border bg-surface px-3 py-2.5 shadow-md",
        TONES[toast.tone],
      )}
    >
      <Icon className="mt-0.5 h-4 w-4 shrink-0" />
      <span className="flex-1 text-body text-fg">{toast.message}</span>
      <button
        onClick={() => dispatch(dismissToast(toast.id))}
        className="text-fg-muted hover:text-fg"
        aria-label="Dismiss"
      >
        <X className="h-3.5 w-3.5" />
      </button>
    </div>
  );
}

/** Renders active toasts bottom-right (ARCHITECTURE.md §3). Mounted once in AppShell. */
export function ToastHost() {
  const items = useAppSelector((s) => s.toast.items);
  return (
    <div className="pointer-events-none fixed bottom-4 right-4 z-[60] flex w-80 flex-col gap-2">
      {items.map((t) => (
        <div key={t.id} className="pointer-events-auto">
          <ToastItem toast={t} />
        </div>
      ))}
    </div>
  );
}

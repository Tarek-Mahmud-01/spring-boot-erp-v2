import { Moon, Sun } from "lucide-react";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { toggleTheme } from "@/app/store/themeSlice";

/** Light/dark toggle (ARCHITECTURE.md §3.3 — dark mode via data-theme). */
export function ThemeToggle({ className }: { className?: string }) {
  const dispatch = useAppDispatch();
  const resolved = useAppSelector((s) => s.theme.resolved);
  const isDark = resolved === "dark";
  const label = isDark ? "Switch to light theme" : "Switch to dark theme";

  return (
    <button
      type="button"
      onClick={() => dispatch(toggleTheme())}
      aria-label={label}
      title={label}
      className={
        "inline-flex h-9 w-9 items-center justify-center rounded-md border border-border bg-surface text-fg-muted transition-colors hover:bg-surface-muted hover:text-fg focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary " +
        (className ?? "")
      }
    >
      {isDark ? <Moon className="h-4 w-4" /> : <Sun className="h-4 w-4" />}
    </button>
  );
}

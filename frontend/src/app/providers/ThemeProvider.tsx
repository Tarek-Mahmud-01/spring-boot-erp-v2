import { type ReactNode, useEffect } from "react";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { systemChanged } from "@/app/store/themeSlice";

/**
 * Applies the resolved theme to <html data-theme> and keeps it in sync with the
 * OS while the preference is "system" (ARCHITECTURE.md §3.3).
 */
export function ThemeProvider({ children }: { children: ReactNode }) {
  const dispatch = useAppDispatch();
  const resolved = useAppSelector((s) => s.theme.resolved);

  useEffect(() => {
    document.documentElement.setAttribute("data-theme", resolved);
  }, [resolved]);

  useEffect(() => {
    const mq = window.matchMedia("(prefers-color-scheme: dark)");
    const onChange = () => dispatch(systemChanged());
    mq.addEventListener("change", onChange);
    return () => mq.removeEventListener("change", onChange);
  }, [dispatch]);

  return <>{children}</>;
}

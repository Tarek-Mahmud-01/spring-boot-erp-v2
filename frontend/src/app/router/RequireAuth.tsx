import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { Loader2 } from "lucide-react";
import { useAppSelector } from "@/shared/hooks/redux";
import { ROUTES } from "@/shared/constants/routePaths";

/**
 * Gate for authenticated areas. Waits for the startup "who am I" to resolve
 * (§3.2) before deciding, so a valid persisted session isn't bounced to login
 * on reload. Unauthenticated users are redirected to /login, preserving the
 * attempted path for post-login return.
 */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { bootstrapped, user } = useAppSelector((s) => s.auth);
  const location = useLocation();

  if (!bootstrapped) {
    return (
      <div className="flex h-screen items-center justify-center bg-bg">
        <Loader2 className="h-6 w-6 animate-spin text-fg-muted" aria-label="Loading" />
      </div>
    );
  }

  if (!user) {
    return <Navigate to={ROUTES.LOGIN} state={{ from: location.pathname }} replace />;
  }

  return <>{children}</>;
}

import { Suspense } from "react";
import { Outlet } from "react-router-dom";
import { Loader2 } from "lucide-react";
import { Header } from "./Header";
import { Sidebar } from "./Sidebar";
import { ToastHost } from "@/shared/components/ToastHost";

/**
 * Persistent application shell (ARCHITECTURE.md §3.2). Mounts ONCE — Header and
 * Sidebar never remount on navigation; only the <Outlet/> content swaps, inside
 * a <Suspense> so each lazily-loaded page shows a content-area spinner while its
 * chunk loads. No eager page code.
 */
export function AppShell() {
  return (
    <div className="flex h-screen overflow-hidden bg-bg">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <Header />
        <main className="flex-1 overflow-auto p-6">
          <Suspense
            fallback={
              <div className="flex h-full items-center justify-center">
                <Loader2 className="h-6 w-6 animate-spin text-fg-muted" aria-label="Loading" />
              </div>
            }
          >
            <Outlet />
          </Suspense>
        </main>
      </div>
      <ToastHost />
    </div>
  );
}

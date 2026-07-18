import { LogOut } from "lucide-react";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { logout } from "@/app/store/authSlice";
import { ThemeToggle } from "@/shared/components/ThemeToggle";
import { Button } from "@/shared/components/Button";

/** Persistent header (ARCHITECTURE.md §3.2). User identity + theme + sign out. */
export function Header() {
  const dispatch = useAppDispatch();
  const user = useAppSelector((s) => s.auth.user);

  return (
    <header className="flex h-14 items-center justify-between border-b border-border bg-surface px-4">
      <div className="text-body-strong font-semibold text-fg">ZATCA Phase 2</div>
      <div className="flex items-center gap-3">
        <ThemeToggle />
        {user && (
          <>
            <div className="text-right leading-tight">
              <div className="text-small font-semibold text-fg">{user.fullName}</div>
              <div className="text-[11px] text-fg-muted">{user.username}</div>
            </div>
            <Button variant="ghost" size="sm" onClick={() => dispatch(logout())} aria-label="Sign out">
              <LogOut className="h-4 w-4" />
            </Button>
          </>
        )}
      </div>
    </header>
  );
}

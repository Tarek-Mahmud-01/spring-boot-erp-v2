import { useAppSelector } from "@/shared/hooks/redux";
import { Card, CardBody, CardHeader } from "@/shared/components/Card";

/**
 * Dashboard landing page (composition only — ARCHITECTURE.md §6 cap ≤150).
 * Real widgets load independently later (§3.2); this is the shell's home.
 */
export default function DashboardPage() {
  const user = useAppSelector((s) => s.auth.user);

  return (
    <div className="flex flex-col gap-4">
      <div>
        <h1 className="text-h1 text-fg">Dashboard</h1>
        <p className="mt-1 text-body text-fg-muted">
          Welcome back{user ? `, ${user.fullName}` : ""}.
        </p>
      </div>
      <Card>
        <CardHeader>
          <h2 className="text-h4 text-fg">Getting started</h2>
        </CardHeader>
        <CardBody>
          <p className="text-body text-fg-muted">
            The foundation is in place. Module widgets will populate this space as they come online.
          </p>
        </CardBody>
      </Card>
    </div>
  );
}

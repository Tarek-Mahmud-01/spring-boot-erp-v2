import { useEffect } from "react";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { usePermission } from "@/shared/hooks/usePermission";
import { Card, CardBody, CardHeader } from "@/shared/components/Card";
import { StatusPill } from "@/shared/components/StatusPill";
import { fetchCompany } from "../slice/companySlice";
import { CompanyForm } from "../components/CompanyForm";

/**
 * Company settings — a single-record ("singleton") settings page (ARCHITECTURE.md
 * §6, composition ≤150). Fetches the current company on mount and renders an
 * edit-in-place form. All form/save logic lives in <CompanyForm>.
 */
export default function CompanyPage() {
  const dispatch = useAppDispatch();
  const { has } = usePermission();
  const canRead = has("settings.company.read");
  const canWrite = has("settings.company.write");
  const { company, loading } = useAppSelector((s) => s.company);

  useEffect(() => {
    if (canRead) void dispatch(fetchCompany());
  }, [dispatch, canRead]);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-h1 text-fg">Company settings</h1>
          <p className="mt-1 text-body text-fg-muted">
            Your organization's legal, tax, and compliance details.
          </p>
        </div>
        {company && (
          <StatusPill
            label={company.status === "active" ? "Active" : "Inactive"}
            tone={company.status === "active" ? "success" : "neutral"}
          />
        )}
      </div>

      <Card>
        <CardHeader>
          <h2 className="text-h4 text-fg">{company ? company.legalName : "Company"}</h2>
        </CardHeader>
        <CardBody>
          {loading && !company ? (
            <p className="text-body text-fg-muted">Loading…</p>
          ) : !canRead ? (
            <p className="text-body text-fg-muted">You don't have permission to view company settings.</p>
          ) : !company ? (
            <p className="text-body text-fg-muted">
              No company has been set up yet. Contact your administrator to create one.
            </p>
          ) : (
            <CompanyForm company={company} canWrite={canWrite} />
          )}
        </CardBody>
      </Card>
    </div>
  );
}

import { useEffect, useState } from "react";
import { Plus, Pencil, Trash2 } from "lucide-react";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { usePermission } from "@/shared/hooks/usePermission";
import { PERMISSIONS } from "@/shared/constants/permissionCodes";
import { DataTable, type Column } from "@/shared/components/DataTable";
import { StatusPill } from "@/shared/components/StatusPill";
import { Button } from "@/shared/components/Button";
import { fetchCompany } from "@/modules/settings/company/slice/companySlice";
import { fetchAccounts, setQuery, deleteAccount } from "../slice/accountSlice";
import { AccountFormModal } from "../components/AccountFormModal";
import type { Account } from "../api/accountApi";

/** Chart of accounts — nested-set tree, indented by depth (ARCHITECTURE.md §6 ≤150). */
export default function ChartOfAccountsPage() {
  const dispatch = useAppDispatch();
  const { has } = usePermission();
  const canWrite = has(PERMISSIONS.COA_WRITE);
  const companyId = useAppSelector((s) => s.company.company?.id);
  const { page, query, loading } = useAppSelector((s) => s.account);
  const [editing, setEditing] = useState<Account | null>(null);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    void dispatch(fetchCompany());
  }, [dispatch]);

  useEffect(() => {
    if (companyId) void dispatch(fetchAccounts({ companyId, query }));
  }, [dispatch, companyId, query]);

  const openCreate = () => {
    setEditing(null);
    setOpen(true);
  };

  const columns: Column<Account>[] = [
    {
      key: "code",
      header: "Code",
      render: (a) => (
        <span className="font-semibold" style={{ paddingLeft: `${a.depth * 16}px` }}>
          {a.code}
        </span>
      ),
    },
    { key: "name", header: "Name", render: (a) => a.name },
    { key: "type", header: "Type", render: (a) => a.type },
    { key: "postingType", header: "Posting", render: (a) => a.postingType },
    {
      key: "status",
      header: "Status",
      render: (a) => <StatusPill label={a.status} tone={a.status === "active" ? "success" : "neutral"} />,
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (a) =>
        canWrite ? (
          <div className="flex justify-end gap-1">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                setEditing(a);
                setOpen(true);
              }}
              aria-label="Edit"
            >
              <Pencil className="h-4 w-4" />
            </Button>
            {companyId && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => dispatch(deleteAccount({ id: a.id, companyId }))}
                aria-label="Delete"
              >
                <Trash2 className="h-4 w-4" />
              </Button>
            )}
          </div>
        ) : null,
    },
  ];

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-h1 text-fg">Chart of accounts</h1>
          <p className="mt-1 text-body text-fg-muted">Ledger account tree — types, posting rules, and status.</p>
        </div>
        {canWrite && companyId && (
          <Button onClick={openCreate}>
            <Plus className="h-4 w-4" />
            New account
          </Button>
        )}
      </div>
      {!companyId ? (
        <p className="text-body text-fg-muted">No company configured yet.</p>
      ) : (
        <DataTable
          columns={columns}
          page={page}
          loading={loading}
          rowKey={(a) => a.id}
          onPageChange={(pg) => dispatch(setQuery({ page: pg }))}
          emptyMessage="No accounts yet."
        />
      )}
      {companyId && (
        <AccountFormModal open={open} onOpenChange={setOpen} editing={editing} companyId={companyId} />
      )}
    </div>
  );
}

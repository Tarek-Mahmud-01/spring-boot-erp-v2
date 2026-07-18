import { useEffect, useState } from "react";
import { Plus, Pencil, Hash } from "lucide-react";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { usePermission } from "@/shared/hooks/usePermission";
import { PERMISSIONS } from "@/shared/constants/permissionCodes";
import { DataTable, type Column } from "@/shared/components/DataTable";
import { StatusPill } from "@/shared/components/StatusPill";
import { Button } from "@/shared/components/Button";
import { fetchNumberingRules, setQuery } from "../slice/numberingSlice";
import { NumberingFormModal } from "../components/NumberingFormModal";
import { NumberingAllocateModal } from "../components/NumberingAllocateModal";
import type { NumberingRule } from "../api/numberingApi";

/** Numbering rules list + CRUD (composition only — ARCHITECTURE.md §6 ≤150). */
export default function NumberingPage() {
  const dispatch = useAppDispatch();
  const { has } = usePermission();
  const canWrite = has(PERMISSIONS.NUMBERING_WRITE);
  const { page, query, loading } = useAppSelector((s) => s.numbering);
  const [editing, setEditing] = useState<NumberingRule | null>(null);
  const [open, setOpen] = useState(false);
  const [allocating, setAllocating] = useState<NumberingRule | null>(null);

  useEffect(() => {
    void dispatch(fetchNumberingRules(query));
  }, [dispatch, query]);

  const openCreate = () => {
    setEditing(null);
    setOpen(true);
  };
  const openEdit = (r: NumberingRule) => {
    setEditing(r);
    setOpen(true);
  };

  const columns: Column<NumberingRule>[] = [
    { key: "documentType", header: "Document", render: (r) => <span className="font-semibold">{r.documentType}</span> },
    { key: "prefix", header: "Prefix", render: (r) => r.prefix || "—" },
    { key: "padding", header: "Padding", align: "center", render: (r) => r.padding },
    {
      key: "resetCadence",
      header: "Reset",
      render: (r) => <StatusPill label={r.resetCadence} tone="neutral" />,
    },
    { key: "currentValue", header: "Current", align: "right", render: (r) => r.currentValue },
    { key: "totalIssued", header: "Issued", align: "right", render: (r) => r.totalIssued },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (r) =>
        canWrite ? (
          <div className="flex justify-end gap-1">
            <Button variant="ghost" size="sm" onClick={() => setAllocating(r)} aria-label="Allocate number">
              <Hash className="h-4 w-4" />
            </Button>
            <Button variant="ghost" size="sm" onClick={() => openEdit(r)} aria-label="Edit">
              <Pencil className="h-4 w-4" />
            </Button>
          </div>
        ) : null,
    },
  ];

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-h1 text-fg">Numbering rules</h1>
          <p className="mt-1 text-body text-fg-muted">Per-document sequence numbering with resettable windows.</p>
        </div>
        {canWrite && (
          <Button onClick={openCreate}>
            <Plus className="h-4 w-4" />
            New rule
          </Button>
        )}
      </div>
      <DataTable
        columns={columns}
        page={page}
        loading={loading}
        rowKey={(r) => r.id}
        onPageChange={(p) => dispatch(setQuery({ page: p }))}
        emptyMessage="No numbering rules yet."
      />
      <NumberingFormModal open={open} onOpenChange={setOpen} editing={editing} />
      <NumberingAllocateModal
        open={Boolean(allocating)}
        onOpenChange={(v) => !v && setAllocating(null)}
        rule={allocating}
      />
    </div>
  );
}

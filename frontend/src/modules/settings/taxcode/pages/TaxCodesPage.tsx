import { useEffect, useState } from "react";
import { Plus, Pencil, Trash2 } from "lucide-react";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { usePermission } from "@/shared/hooks/usePermission";
import { PERMISSIONS } from "@/shared/constants/permissionCodes";
import { DataTable, type Column } from "@/shared/components/DataTable";
import { StatusPill } from "@/shared/components/StatusPill";
import { Button } from "@/shared/components/Button";
import { fetchTaxCodes, setQuery, deleteTaxCode } from "../slice/taxcodeSlice";
import { TaxCodeFormModal } from "../components/TaxCodeFormModal";
import type { TaxCode } from "../api/taxcodeApi";

/** Tax codes list + CRUD (composition only — ARCHITECTURE.md §6 ≤150). */
export default function TaxCodesPage() {
  const dispatch = useAppDispatch();
  const { has } = usePermission();
  const canWrite = has(PERMISSIONS.TAXCODE_WRITE);
  const { page, query, loading } = useAppSelector((s) => s.taxcode);
  const [editing, setEditing] = useState<TaxCode | null>(null);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    void dispatch(fetchTaxCodes(query));
  }, [dispatch, query]);

  const openCreate = () => {
    setEditing(null);
    setOpen(true);
  };
  const openEdit = (t: TaxCode) => {
    setEditing(t);
    setOpen(true);
  };

  const columns: Column<TaxCode>[] = [
    { key: "code", header: "Code", render: (t) => <span className="font-semibold">{t.code}</span> },
    { key: "description", header: "Description", render: (t) => t.description },
    { key: "rate", header: "Rate %", align: "right", render: (t) => t.ratePercent },
    { key: "gstTreatment", header: "GST treatment", render: (t) => t.gstTreatment.replace(/_/g, " ") },
    {
      key: "status",
      header: "Status",
      render: (t) => <StatusPill label={t.status} tone={t.status === "ACTIVE" ? "success" : "neutral"} />,
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (t) =>
        canWrite ? (
          <div className="flex justify-end gap-1">
            <Button variant="ghost" size="sm" onClick={() => openEdit(t)} aria-label="Edit">
              <Pencil className="h-4 w-4" />
            </Button>
            <Button variant="ghost" size="sm" onClick={() => dispatch(deleteTaxCode(t.id))} aria-label="Delete">
              <Trash2 className="h-4 w-4" />
            </Button>
          </div>
        ) : null,
    },
  ];

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-h1 text-fg">Tax codes</h1>
          <p className="mt-1 text-body text-fg-muted">GST tax codes applied across sales and purchases.</p>
        </div>
        {canWrite && (
          <Button onClick={openCreate}>
            <Plus className="h-4 w-4" />
            New tax code
          </Button>
        )}
      </div>
      <DataTable
        columns={columns}
        page={page}
        loading={loading}
        rowKey={(t) => t.id}
        onPageChange={(p) => dispatch(setQuery({ page: p }))}
        emptyMessage="No tax codes yet."
      />
      <TaxCodeFormModal open={open} onOpenChange={setOpen} editing={editing} />
    </div>
  );
}

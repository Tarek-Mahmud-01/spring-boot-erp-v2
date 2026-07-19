import { useEffect, useState } from "react";
import { Plus, Pencil, Trash2 } from "lucide-react";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { usePermission } from "@/shared/hooks/usePermission";
import { PERMISSIONS } from "@/shared/constants/permissionCodes";
import { DataTable, type Column } from "@/shared/components/DataTable";
import { StatusPill } from "@/shared/components/StatusPill";
import { Button } from "@/shared/components/Button";
import { formatMoney } from "@/shared/utils/format";
import { fetchSuppliers, setQuery, deleteSupplier, setSupplierStatus } from "../slice/supplierSlice";
import { SupplierFormModal } from "../components/SupplierFormModal";
import type { Supplier, SupplierStatus } from "../api/supplierApi";

const STATUS_TONE: Record<SupplierStatus, "success" | "neutral" | "danger"> = {
  Active: "success",
  Inactive: "neutral",
  Blocked: "danger",
};

/** Suppliers list + CRUD + status transitions (ARCHITECTURE.md §6 ≤150). */
export default function SuppliersPage() {
  const dispatch = useAppDispatch();
  const { has } = usePermission();
  const canWrite = has(PERMISSIONS.SUPPLIER_WRITE);
  const { page, query, loading } = useAppSelector((s) => s.supplier);
  const [editing, setEditing] = useState<Supplier | null>(null);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    void dispatch(fetchSuppliers(query));
  }, [dispatch, query]);

  const openCreate = () => {
    setEditing(null);
    setOpen(true);
  };

  const onStatusChange = (s: Supplier, next: SupplierStatus) => {
    if (next === "Blocked") {
      const reason = window.prompt(`Reason for blocking ${s.name}?`);
      if (!reason) return;
      void dispatch(setSupplierStatus({ id: s.id, status: next, blockReason: reason }));
      return;
    }
    void dispatch(setSupplierStatus({ id: s.id, status: next }));
  };

  const columns: Column<Supplier>[] = [
    { key: "code", header: "Code", render: (s) => <span className="font-semibold">{s.code}</span> },
    { key: "name", header: "Name", render: (s) => s.name },
    { key: "type", header: "Type", render: (s) => s.type },
    {
      key: "creditLimit",
      header: "Credit limit",
      align: "right",
      render: (s) =>
        s.creditLimitCurrency ? formatMoney({ amountMinor: s.creditLimitAmount, currency: s.creditLimitCurrency }) : "—",
    },
    {
      key: "status",
      header: "Status",
      render: (s) => <StatusPill label={s.status} tone={STATUS_TONE[s.status]} />,
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (s) =>
        canWrite ? (
          <div className="flex justify-end gap-1">
            {(["Active", "Inactive", "Blocked"] as SupplierStatus[])
              .filter((st) => st !== s.status)
              .map((st) => (
                <Button key={st} variant="ghost" size="sm" onClick={() => onStatusChange(s, st)}>
                  {st}
                </Button>
              ))}
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                setEditing(s);
                setOpen(true);
              }}
              aria-label="Edit"
            >
              <Pencil className="h-4 w-4" />
            </Button>
            <Button variant="ghost" size="sm" onClick={() => dispatch(deleteSupplier(s.id))} aria-label="Delete">
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
          <h1 className="text-h1 text-fg">Suppliers</h1>
          <p className="mt-1 text-body text-fg-muted">Supplier master — contacts, terms, and status.</p>
        </div>
        {canWrite && (
          <Button onClick={openCreate}>
            <Plus className="h-4 w-4" />
            New supplier
          </Button>
        )}
      </div>
      <DataTable
        columns={columns}
        page={page}
        loading={loading}
        rowKey={(s) => s.id}
        onPageChange={(pg) => dispatch(setQuery({ page: pg }))}
        emptyMessage="No suppliers yet."
      />
      <SupplierFormModal open={open} onOpenChange={setOpen} editing={editing} />
    </div>
  );
}

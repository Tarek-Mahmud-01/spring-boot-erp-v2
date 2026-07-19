import { useEffect, useState } from "react";
import { Plus, Trash2, CheckCircle2 } from "lucide-react";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { usePermission } from "@/shared/hooks/usePermission";
import { PERMISSIONS } from "@/shared/constants/permissionCodes";
import { DataTable, type Column } from "@/shared/components/DataTable";
import { StatusPill } from "@/shared/components/StatusPill";
import { Button } from "@/shared/components/Button";
import { formatMoney } from "@/shared/utils/format";
import { fetchBills, setQuery, deleteBill, approveBill } from "../slice/billSlice";
import { BillFormModal } from "../components/BillFormModal";
import type { SupplierBill, BillStatus } from "../api/billApi";

const STATUS_TONE: Record<BillStatus, "success" | "neutral" | "warning" | "danger" | "info"> = {
  Draft: "neutral",
  Received: "info",
  Approved: "success",
  Paid: "success",
  "Partially Received": "warning",
  Cancelled: "danger",
  "Invoiced Not Received": "warning",
};

/** Supplier bills list + create + approve (ARCHITECTURE.md §6 ≤150). */
export default function BillsPage() {
  const dispatch = useAppDispatch();
  const { has } = usePermission();
  const canWrite = has(PERMISSIONS.BILL_WRITE);
  const { page, query, loading } = useAppSelector((s) => s.bill);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    void dispatch(fetchBills(query));
  }, [dispatch, query]);

  const columns: Column<SupplierBill>[] = [
    { key: "number", header: "Bill #", render: (b) => <span className="font-semibold">{b.number}</span> },
    { key: "supplierBillNo", header: "Supplier bill #", render: (b) => b.supplierBillNo ?? "—" },
    { key: "billDate", header: "Bill date", render: (b) => new Date(b.billDate).toLocaleDateString() },
    {
      key: "total",
      header: "Total",
      align: "right",
      render: (b) => formatMoney({ amountMinor: b.totalAmount, currency: b.currency }),
    },
    {
      key: "status",
      header: "Status",
      render: (b) => <StatusPill label={b.status} tone={STATUS_TONE[b.status]} />,
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (b) =>
        canWrite ? (
          <div className="flex justify-end gap-1">
            {(b.status === "Draft" || b.status === "Received") && (
              <Button variant="ghost" size="sm" onClick={() => dispatch(approveBill({ id: b.id, version: b.version }))}>
                <CheckCircle2 className="h-4 w-4" />
                Approve
              </Button>
            )}
            {b.status === "Draft" && (
              <Button variant="ghost" size="sm" onClick={() => dispatch(deleteBill(b.id))} aria-label="Delete">
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
          <h1 className="text-h1 text-fg">Supplier bills</h1>
          <p className="mt-1 text-body text-fg-muted">Accounts payable — 3-way match and approval.</p>
        </div>
        {canWrite && (
          <Button onClick={() => setOpen(true)}>
            <Plus className="h-4 w-4" />
            New bill
          </Button>
        )}
      </div>
      <DataTable
        columns={columns}
        page={page}
        loading={loading}
        rowKey={(b) => b.id}
        onPageChange={(pg) => dispatch(setQuery({ page: pg }))}
        emptyMessage="No supplier bills yet."
      />
      <BillFormModal open={open} onOpenChange={setOpen} />
    </div>
  );
}

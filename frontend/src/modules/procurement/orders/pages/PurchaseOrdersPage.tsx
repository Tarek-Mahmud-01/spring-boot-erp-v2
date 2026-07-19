import { useEffect, useState } from "react";
import { Plus, Trash2 } from "lucide-react";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { usePermission } from "@/shared/hooks/usePermission";
import { PERMISSIONS } from "@/shared/constants/permissionCodes";
import { DataTable, type Column } from "@/shared/components/DataTable";
import { StatusPill } from "@/shared/components/StatusPill";
import { Button } from "@/shared/components/Button";
import { formatMoney } from "@/shared/utils/format";
import { fetchPurchaseOrders, setQuery, deletePurchaseOrder, transitionPurchaseOrder } from "../slice/purchaseOrderSlice";
import { PurchaseOrderFormModal } from "../components/PurchaseOrderFormModal";
import type { PurchaseOrder, PoStatus } from "../api/purchaseOrderApi";

const STATUS_TONE: Record<PoStatus, "success" | "neutral" | "warning" | "danger" | "info"> = {
  Draft: "neutral",
  Submitted: "info",
  Approved: "success",
  Sent: "info",
  "Partially Received": "warning",
  Received: "success",
  Closed: "neutral",
  Cancelled: "danger",
};

const NEXT_STATUS: Record<PoStatus, PoStatus[]> = {
  Draft: ["Submitted", "Approved", "Cancelled"],
  Submitted: ["Approved", "Cancelled"],
  Approved: ["Sent", "Received", "Cancelled"],
  Sent: ["Received", "Closed"],
  "Partially Received": ["Received", "Closed"],
  Received: ["Closed"],
  Closed: [],
  Cancelled: [],
};

/** Purchase orders list + create + workflow transitions (ARCHITECTURE.md §6 ≤150). */
export default function PurchaseOrdersPage() {
  const dispatch = useAppDispatch();
  const { has } = usePermission();
  const canWrite = has(PERMISSIONS.PROCUREMENT_ORDER_WRITE);
  const { page, query, loading } = useAppSelector((s) => s.purchaseOrder);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    void dispatch(fetchPurchaseOrders(query));
  }, [dispatch, query]);

  const lineTotal = (po: PurchaseOrder) =>
    po.lines.reduce((sum, l) => sum + l.lineTotalAmount, 0);

  const columns: Column<PurchaseOrder>[] = [
    { key: "number", header: "PO #", render: (p) => <span className="font-semibold">{p.number}</span> },
    { key: "supplier", header: "Supplier ID", render: (p) => p.supplierId },
    { key: "date", header: "Date", render: (p) => new Date(p.poDate).toLocaleDateString() },
    {
      key: "total",
      header: "Total",
      align: "right",
      render: (p) => formatMoney({ amountMinor: lineTotal(p), currency: p.currency }),
    },
    {
      key: "status",
      header: "Status",
      render: (p) => <StatusPill label={p.status} tone={STATUS_TONE[p.status]} />,
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (p) =>
        canWrite ? (
          <div className="flex justify-end gap-1">
            {NEXT_STATUS[p.status].map((next) => (
              <Button
                key={next}
                variant="ghost"
                size="sm"
                onClick={() => dispatch(transitionPurchaseOrder({ id: p.id, toStatus: next }))}
              >
                {next}
              </Button>
            ))}
            {p.status === "Draft" && (
              <Button variant="ghost" size="sm" onClick={() => dispatch(deletePurchaseOrder(p.id))} aria-label="Delete">
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
          <h1 className="text-h1 text-fg">Purchase orders</h1>
          <p className="mt-1 text-body text-fg-muted">Orders to suppliers — draft through receipt and close.</p>
        </div>
        {canWrite && (
          <Button onClick={() => setOpen(true)}>
            <Plus className="h-4 w-4" />
            New order
          </Button>
        )}
      </div>
      <DataTable
        columns={columns}
        page={page}
        loading={loading}
        rowKey={(p) => p.id}
        onPageChange={(pg) => dispatch(setQuery({ page: pg }))}
        emptyMessage="No purchase orders yet."
      />
      <PurchaseOrderFormModal open={open} onOpenChange={setOpen} />
    </div>
  );
}

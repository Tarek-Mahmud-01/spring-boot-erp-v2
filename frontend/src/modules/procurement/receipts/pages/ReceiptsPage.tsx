import { useEffect, useState } from "react";
import { Plus, Trash2, CheckCircle2 } from "lucide-react";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { usePermission } from "@/shared/hooks/usePermission";
import { PERMISSIONS } from "@/shared/constants/permissionCodes";
import { DataTable, type Column } from "@/shared/components/DataTable";
import { StatusPill } from "@/shared/components/StatusPill";
import { Button } from "@/shared/components/Button";
import { fetchReceipts, setQuery, deleteReceipt, transitionReceipt, confirmReceipt } from "../slice/receiptSlice";
import { ReceiptFormModal } from "../components/ReceiptFormModal";
import type { GoodsReceipt, GrnStatus } from "../api/receiptApi";

const STATUS_TONE: Record<GrnStatus, "success" | "neutral" | "warning" | "info"> = {
  Draft: "neutral",
  Approved: "info",
  "Partially Received": "warning",
  Received: "success",
  Confirmed: "success",
};

/** Goods receipts (GRN) list + create + transition/confirm (ARCHITECTURE.md §6 ≤150). */
export default function ReceiptsPage() {
  const dispatch = useAppDispatch();
  const { has } = usePermission();
  const canWrite = has(PERMISSIONS.RECEIPT_WRITE);
  const { page, query, loading } = useAppSelector((s) => s.receipt);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    void dispatch(fetchReceipts(query));
  }, [dispatch, query]);

  const columns: Column<GoodsReceipt>[] = [
    { key: "number", header: "GRN #", render: (g) => <span className="font-semibold">{g.number}</span> },
    { key: "po", header: "PO ID", render: (g) => g.poId },
    { key: "receivedAt", header: "Received at", render: (g) => new Date(g.receivedAt).toLocaleDateString() },
    { key: "deliveryNote", header: "Delivery note", render: (g) => g.deliveryNoteNo ?? "—" },
    {
      key: "status",
      header: "Status",
      render: (g) => <StatusPill label={g.status} tone={STATUS_TONE[g.status]} />,
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (g) => {
        const isReceived = g.status === "Received" || g.status === "Confirmed";
        return canWrite ? (
          <div className="flex justify-end gap-1">
            {g.status === "Draft" && (
              <Button variant="ghost" size="sm" onClick={() => dispatch(transitionReceipt({ id: g.id, toStatus: "Approved" }))}>
                Approve
              </Button>
            )}
            {!isReceived && (
              <Button variant="ghost" size="sm" onClick={() => dispatch(confirmReceipt(g.id))}>
                <CheckCircle2 className="h-4 w-4" />
                Confirm
              </Button>
            )}
            {g.status === "Draft" && (
              <Button variant="ghost" size="sm" onClick={() => dispatch(deleteReceipt(g.id))} aria-label="Delete">
                <Trash2 className="h-4 w-4" />
              </Button>
            )}
          </div>
        ) : null;
      },
    },
  ];

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-h1 text-fg">Goods receipts</h1>
          <p className="mt-1 text-body text-fg-muted">GRNs — receive against a PO and post stock.</p>
        </div>
        {canWrite && (
          <Button onClick={() => setOpen(true)}>
            <Plus className="h-4 w-4" />
            New receipt
          </Button>
        )}
      </div>
      <DataTable
        columns={columns}
        page={page}
        loading={loading}
        rowKey={(g) => g.id}
        onPageChange={(pg) => dispatch(setQuery({ page: pg }))}
        emptyMessage="No goods receipts yet."
      />
      <ReceiptFormModal open={open} onOpenChange={setOpen} />
    </div>
  );
}

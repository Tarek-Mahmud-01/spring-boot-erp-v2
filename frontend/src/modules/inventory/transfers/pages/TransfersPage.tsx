import { useEffect, useState } from "react";
import { Plus, Trash2, CheckCircle2, PackageCheck } from "lucide-react";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { usePermission } from "@/shared/hooks/usePermission";
import { PERMISSIONS } from "@/shared/constants/permissionCodes";
import { DataTable, type Column } from "@/shared/components/DataTable";
import { StatusPill } from "@/shared/components/StatusPill";
import { Button } from "@/shared/components/Button";
import { fetchTransfers, setQuery, deleteTransfer, confirmTransfer, completeTransfer } from "../slice/transferSlice";
import { TransferFormModal } from "../components/TransferFormModal";
import type { StockTransfer, TransferStatus } from "../api/transferApi";

const STATUS_TONE: Record<TransferStatus, "success" | "neutral" | "warning" | "info"> = {
  Draft: "neutral",
  Approved: "info",
  "Partially Complete": "warning",
  Complete: "success",
};

/** Stock transfers list + create + confirm/complete (ARCHITECTURE.md §6 ≤150). */
export default function TransfersPage() {
  const dispatch = useAppDispatch();
  const { has } = usePermission();
  const canWrite = has(PERMISSIONS.TRANSFER_WRITE);
  const { page, query, loading } = useAppSelector((s) => s.transfer);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    void dispatch(fetchTransfers(query));
  }, [dispatch, query]);

  const columns: Column<StockTransfer>[] = [
    { key: "number", header: "Transfer #", render: (t) => <span className="font-semibold">{t.number}</span> },
    { key: "source", header: "Source", render: (t) => t.sourceLocationId },
    { key: "destination", header: "Destination", render: (t) => t.destinationLocationId },
    {
      key: "status",
      header: "Status",
      render: (t) => <StatusPill label={t.status} tone={STATUS_TONE[t.status]} />,
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (t) =>
        canWrite ? (
          <div className="flex justify-end gap-1">
            {t.status === "Draft" && (
              <Button variant="ghost" size="sm" onClick={() => dispatch(confirmTransfer(t.id))}>
                <CheckCircle2 className="h-4 w-4" />
                Confirm
              </Button>
            )}
            {(t.status === "Approved" || t.status === "Partially Complete") && (
              <Button variant="ghost" size="sm" onClick={() => dispatch(completeTransfer(t.id))}>
                <PackageCheck className="h-4 w-4" />
                Complete
              </Button>
            )}
            {t.status === "Draft" && (
              <Button variant="ghost" size="sm" onClick={() => dispatch(deleteTransfer(t.id))} aria-label="Delete">
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
          <h1 className="text-h1 text-fg">Stock transfers</h1>
          <p className="mt-1 text-body text-fg-muted">Inter-location movements — confirm, then receive.</p>
        </div>
        {canWrite && (
          <Button onClick={() => setOpen(true)}>
            <Plus className="h-4 w-4" />
            New transfer
          </Button>
        )}
      </div>
      <DataTable
        columns={columns}
        page={page}
        loading={loading}
        rowKey={(t) => t.id}
        onPageChange={(pg) => dispatch(setQuery({ page: pg }))}
        emptyMessage="No transfers yet."
      />
      <TransferFormModal open={open} onOpenChange={setOpen} />
    </div>
  );
}

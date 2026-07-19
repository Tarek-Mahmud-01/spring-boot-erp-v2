import { useEffect, useState } from "react";
import { Plus, Trash2, CheckCircle2, Send } from "lucide-react";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { usePermission } from "@/shared/hooks/usePermission";
import { PERMISSIONS } from "@/shared/constants/permissionCodes";
import { DataTable, type Column } from "@/shared/components/DataTable";
import { StatusPill } from "@/shared/components/StatusPill";
import { Button } from "@/shared/components/Button";
import { fetchAdjustments, setQuery, deleteAdjustment, approveAdjustment, postAdjustment } from "../slice/adjustmentSlice";
import { AdjustmentFormModal } from "../components/AdjustmentFormModal";
import type { StockAdjustment, AdjustmentStatus } from "../api/adjustmentApi";

const STATUS_TONE: Record<AdjustmentStatus, "success" | "neutral" | "warning" | "info" | "danger"> = {
  Draft: "neutral",
  "Pending Approval": "warning",
  Approved: "info",
  Posted: "success",
  Reversed: "danger",
};

/** Stock adjustments list + create + approve/post (ARCHITECTURE.md §6 ≤150). */
export default function AdjustmentsPage() {
  const dispatch = useAppDispatch();
  const { has } = usePermission();
  const canWrite = has(PERMISSIONS.ADJUSTMENT_WRITE);
  const { page, query, loading } = useAppSelector((s) => s.adjustment);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    void dispatch(fetchAdjustments(query));
  }, [dispatch, query]);

  const columns: Column<StockAdjustment>[] = [
    { key: "number", header: "Adj #", render: (a) => <span className="font-semibold">{a.number}</span> },
    { key: "location", header: "Location ID", render: (a) => a.locationId },
    { key: "reason", header: "Reason", render: (a) => a.reason },
    {
      key: "status",
      header: "Status",
      render: (a) => <StatusPill label={a.status} tone={STATUS_TONE[a.status]} />,
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (a) =>
        canWrite ? (
          <div className="flex justify-end gap-1">
            {(a.status === "Draft" || a.status === "Pending Approval") && (
              <Button variant="ghost" size="sm" onClick={() => dispatch(approveAdjustment(a.id))}>
                <CheckCircle2 className="h-4 w-4" />
                Approve
              </Button>
            )}
            {a.status === "Approved" && (
              <Button variant="ghost" size="sm" onClick={() => dispatch(postAdjustment(a.id))}>
                <Send className="h-4 w-4" />
                Post
              </Button>
            )}
            {a.status === "Draft" && (
              <Button variant="ghost" size="sm" onClick={() => dispatch(deleteAdjustment(a.id))} aria-label="Delete">
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
          <h1 className="text-h1 text-fg">Stock adjustments</h1>
          <p className="mt-1 text-body text-fg-muted">Variance corrections — approve, then post to the ledger.</p>
        </div>
        {canWrite && (
          <Button onClick={() => setOpen(true)}>
            <Plus className="h-4 w-4" />
            New adjustment
          </Button>
        )}
      </div>
      <DataTable
        columns={columns}
        page={page}
        loading={loading}
        rowKey={(a) => a.id}
        onPageChange={(pg) => dispatch(setQuery({ page: pg }))}
        emptyMessage="No adjustments yet."
      />
      <AdjustmentFormModal open={open} onOpenChange={setOpen} />
    </div>
  );
}

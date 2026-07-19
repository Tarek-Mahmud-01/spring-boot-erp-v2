import { useEffect, useState } from "react";
import { Plus, Trash2, ArrowRightCircle } from "lucide-react";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { usePermission } from "@/shared/hooks/usePermission";
import { PERMISSIONS } from "@/shared/constants/permissionCodes";
import { DataTable, type Column } from "@/shared/components/DataTable";
import { StatusPill } from "@/shared/components/StatusPill";
import { Button } from "@/shared/components/Button";
import { formatMoney } from "@/shared/utils/format";
import {
  fetchRequisitions,
  setQuery,
  deleteRequisition,
  submitRequisition,
  transitionRequisition,
  convertRequisitionToPo,
} from "../slice/requisitionSlice";
import { RequisitionFormModal } from "../components/RequisitionFormModal";
import type { PurchaseRequisition, PrStatus } from "../api/requisitionApi";

const STATUS_TONE: Record<PrStatus, "success" | "neutral" | "warning" | "danger" | "info"> = {
  Draft: "neutral",
  Submitted: "info",
  "Under Review": "warning",
  "Sent to Supplier": "info",
  Converted: "success",
  Rejected: "danger",
};

/** Purchase requisitions list + create + workflow (ARCHITECTURE.md §6 ≤150). */
export default function RequisitionsPage() {
  const dispatch = useAppDispatch();
  const { has } = usePermission();
  const canWrite = has(PERMISSIONS.PROCUREMENT_REQUISITION_WRITE);
  const canConvert = has(PERMISSIONS.PROCUREMENT_ORDER_WRITE);
  const { page, query, loading } = useAppSelector((s) => s.requisition);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    void dispatch(fetchRequisitions(query));
  }, [dispatch, query]);

  const onReject = (r: PurchaseRequisition) => {
    const reason = window.prompt(`Reason for rejecting ${r.number}?`);
    if (!reason) return;
    void dispatch(transitionRequisition({ id: r.id, toStatus: "Rejected", reason }));
  };

  const columns: Column<PurchaseRequisition>[] = [
    { key: "number", header: "PR #", render: (r) => <span className="font-semibold">{r.number}</span> },
    { key: "location", header: "Location ID", render: (r) => r.locationId },
    {
      key: "total",
      header: "Total",
      align: "right",
      render: (r) => formatMoney({ amountMinor: r.totalAmount, currency: r.currency }),
    },
    {
      key: "status",
      header: "Status",
      render: (r) => <StatusPill label={r.status} tone={STATUS_TONE[r.status]} />,
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (r) =>
        canWrite ? (
          <div className="flex justify-end gap-1">
            {r.status === "Draft" && (
              <>
                <Button variant="ghost" size="sm" onClick={() => dispatch(submitRequisition(r.id))}>
                  Submit
                </Button>
                <Button variant="ghost" size="sm" onClick={() => dispatch(deleteRequisition(r.id))} aria-label="Delete">
                  <Trash2 className="h-4 w-4" />
                </Button>
              </>
            )}
            {r.status === "Submitted" && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => dispatch(transitionRequisition({ id: r.id, toStatus: "Under Review" }))}
              >
                Review
              </Button>
            )}
            {r.status === "Under Review" && (
              <>
                {canConvert && (
                  <Button variant="ghost" size="sm" onClick={() => dispatch(convertRequisitionToPo(r.id))}>
                    <ArrowRightCircle className="h-4 w-4" />
                    To PO
                  </Button>
                )}
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => dispatch(transitionRequisition({ id: r.id, toStatus: "Sent to Supplier" }))}
                >
                  Send
                </Button>
              </>
            )}
            {["Submitted", "Under Review", "Sent to Supplier"].includes(r.status) && (
              <Button variant="ghost" size="sm" onClick={() => onReject(r)}>
                Reject
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
          <h1 className="text-h1 text-fg">Purchase requisitions</h1>
          <p className="mt-1 text-body text-fg-muted">Internal requests — review, approve, and convert to POs.</p>
        </div>
        {canWrite && (
          <Button onClick={() => setOpen(true)}>
            <Plus className="h-4 w-4" />
            New requisition
          </Button>
        )}
      </div>
      <DataTable
        columns={columns}
        page={page}
        loading={loading}
        rowKey={(r) => r.id}
        onPageChange={(pg) => dispatch(setQuery({ page: pg }))}
        emptyMessage="No requisitions yet."
      />
      <RequisitionFormModal open={open} onOpenChange={setOpen} />
    </div>
  );
}

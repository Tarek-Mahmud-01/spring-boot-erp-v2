import { useEffect, useState } from "react";
import { Plus, Trash2, Send, RotateCcw } from "lucide-react";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { usePermission } from "@/shared/hooks/usePermission";
import { PERMISSIONS } from "@/shared/constants/permissionCodes";
import { DataTable, type Column } from "@/shared/components/DataTable";
import { StatusPill } from "@/shared/components/StatusPill";
import { Button } from "@/shared/components/Button";
import { formatMoney } from "@/shared/utils/format";
import { fetchCompany } from "@/modules/settings/company/slice/companySlice";
import {
  fetchJournalEntries,
  setQuery,
  deleteJournalEntry,
  postJournalEntry,
  reverseJournalEntry,
} from "../slice/journalEntrySlice";
import { JournalEntryFormModal } from "../components/JournalEntryFormModal";
import type { JournalEntry, JournalEntryStatus } from "../api/journalEntryApi";

const STATUS_TONE: Record<JournalEntryStatus, "neutral" | "success" | "danger"> = {
  DRAFT: "neutral",
  POSTED: "success",
  REVERSED: "danger",
};

/** GL journal entries list + create + post/reverse (ARCHITECTURE.md §6 ≤150). */
export default function JournalEntriesPage() {
  const dispatch = useAppDispatch();
  const { has } = usePermission();
  const canWrite = has(PERMISSIONS.JOURNAL_WRITE);
  const canPost = has(PERMISSIONS.JOURNAL_POST);
  const companyId = useAppSelector((s) => s.company.company?.id);
  const { page, query, loading } = useAppSelector((s) => s.journalEntry);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    void dispatch(fetchCompany());
  }, [dispatch]);

  useEffect(() => {
    if (companyId) void dispatch(fetchJournalEntries({ companyId, query }));
  }, [dispatch, companyId, query]);

  const onReverse = (e: JournalEntry) => {
    if (!companyId) return;
    const narration = window.prompt(`Reversal narration for ${e.voucherNumber ?? e.id}?`) ?? undefined;
    void dispatch(
      reverseJournalEntry({ id: e.id, companyId, entryDate: new Date().toISOString().slice(0, 10), narration }),
    );
  };

  const columns: Column<JournalEntry>[] = [
    { key: "voucher", header: "Voucher #", render: (e) => <span className="font-semibold">{e.voucherNumber ?? "—"}</span> },
    { key: "type", header: "Type", render: (e) => e.voucherType },
    { key: "date", header: "Date", render: (e) => new Date(e.entryDate).toLocaleDateString() },
    {
      key: "debit",
      header: "Debit",
      align: "right",
      render: (e) => formatMoney({ amountMinor: e.totalDebit, currency: e.lines[0]?.currency ?? "SAR" }),
    },
    {
      key: "status",
      header: "Status",
      render: (e) => <StatusPill label={e.status} tone={STATUS_TONE[e.status]} />,
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (e) =>
        companyId ? (
          <div className="flex justify-end gap-1">
            {e.status === "DRAFT" && canPost && (
              <Button variant="ghost" size="sm" onClick={() => dispatch(postJournalEntry({ id: e.id, companyId }))}>
                <Send className="h-4 w-4" />
                Post
              </Button>
            )}
            {e.status === "POSTED" && canPost && (
              <Button variant="ghost" size="sm" onClick={() => onReverse(e)}>
                <RotateCcw className="h-4 w-4" />
                Reverse
              </Button>
            )}
            {e.status === "DRAFT" && canWrite && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => dispatch(deleteJournalEntry({ id: e.id, companyId }))}
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
          <h1 className="text-h1 text-fg">Journal entries</h1>
          <p className="mt-1 text-body text-fg-muted">General ledger — draft, post, and reverse.</p>
        </div>
        {canWrite && companyId && (
          <Button onClick={() => setOpen(true)}>
            <Plus className="h-4 w-4" />
            New entry
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
          rowKey={(e) => e.id}
          onPageChange={(pg) => dispatch(setQuery({ page: pg }))}
          emptyMessage="No journal entries yet."
        />
      )}
      {companyId && <JournalEntryFormModal open={open} onOpenChange={setOpen} companyId={companyId} />}
    </div>
  );
}

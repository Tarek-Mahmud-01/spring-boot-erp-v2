import { useEffect, useState } from "react";
import { Plus, Pencil, Star, Trash2 } from "lucide-react";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { usePermission } from "@/shared/hooks/usePermission";
import { PERMISSIONS } from "@/shared/constants/permissionCodes";
import { DataTable, type Column } from "@/shared/components/DataTable";
import { StatusPill } from "@/shared/components/StatusPill";
import { Button } from "@/shared/components/Button";
import { fetchCurrencies, setQuery, deleteCurrency, setDefaultCurrency } from "../slice/currencySlice";
import { CurrencyFormModal } from "../components/CurrencyFormModal";
import type { Currency } from "../api/currencyApi";

/** Currencies list + CRUD (composition only — ARCHITECTURE.md §6 ≤150). */
export default function CurrenciesPage() {
  const dispatch = useAppDispatch();
  const { has } = usePermission();
  const canWrite = has(PERMISSIONS.CURRENCY_WRITE);
  const { page, query, loading } = useAppSelector((s) => s.currency);
  const [editing, setEditing] = useState<Currency | null>(null);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    void dispatch(fetchCurrencies(query));
  }, [dispatch, query]);

  const openCreate = () => {
    setEditing(null);
    setOpen(true);
  };
  const openEdit = (c: Currency) => {
    setEditing(c);
    setOpen(true);
  };

  const columns: Column<Currency>[] = [
    {
      key: "code",
      header: "Code",
      render: (c) => (
        <span className="flex items-center gap-1.5 font-semibold">
          {c.code}
          {c.isDefault && <Star className="h-3.5 w-3.5 fill-warning text-warning" aria-label="Default" />}
        </span>
      ),
    },
    { key: "name", header: "Name", render: (c) => c.name },
    { key: "symbol", header: "Symbol", render: (c) => c.symbol },
    { key: "decimals", header: "Decimals", align: "center", render: (c) => c.decimalPlaces },
    {
      key: "status",
      header: "Status",
      render: (c) => <StatusPill label={c.status} tone={c.isActive ? "success" : "neutral"} />,
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (c) =>
        canWrite ? (
          <div className="flex justify-end gap-1">
            {!c.isDefault && (
              <Button variant="ghost" size="sm" onClick={() => dispatch(setDefaultCurrency(c.id))} aria-label="Set default">
                <Star className="h-4 w-4" />
              </Button>
            )}
            <Button variant="ghost" size="sm" onClick={() => openEdit(c)} aria-label="Edit">
              <Pencil className="h-4 w-4" />
            </Button>
            <Button variant="ghost" size="sm" onClick={() => dispatch(deleteCurrency(c.id))} aria-label="Delete">
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
          <h1 className="text-h1 text-fg">Currencies</h1>
          <p className="mt-1 text-body text-fg-muted">ISO-4217 currencies used across the system.</p>
        </div>
        {canWrite && (
          <Button onClick={openCreate}>
            <Plus className="h-4 w-4" />
            New currency
          </Button>
        )}
      </div>
      <DataTable
        columns={columns}
        page={page}
        loading={loading}
        rowKey={(c) => c.id}
        onPageChange={(p) => dispatch(setQuery({ page: p }))}
        emptyMessage="No currencies yet."
      />
      <CurrencyFormModal open={open} onOpenChange={setOpen} editing={editing} />
    </div>
  );
}

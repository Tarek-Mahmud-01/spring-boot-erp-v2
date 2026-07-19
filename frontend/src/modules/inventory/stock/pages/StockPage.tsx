import { useEffect } from "react";
import { Loader2 } from "lucide-react";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { Input } from "@/shared/components/Input";
import { Field } from "@/shared/components/Field";
import { StatusPill } from "@/shared/components/StatusPill";
import { formatMoney } from "@/shared/utils/format";
import { fetchStockOnHand, setQuery } from "../slice/stockSlice";
import type { StockStatus } from "../api/stockApi";

const STATUS_TONE: Record<StockStatus, "success" | "warning" | "danger" | "info"> = {
  AVAILABLE: "success",
  RESERVED: "warning",
  QUARANTINE: "danger",
  IN_TRANSIT: "info",
};

/** Stock on-hand — read-only ledger projection (ARCHITECTURE.md §6 ≤150). */
export default function StockPage() {
  const dispatch = useAppDispatch();
  const { rows, query, loading } = useAppSelector((s) => s.stock);

  useEffect(() => {
    void dispatch(fetchStockOnHand(query));
  }, [dispatch, query]);

  return (
    <div className="flex flex-col gap-4">
      <div>
        <h1 className="text-h1 text-fg">Stock on hand</h1>
        <p className="mt-1 text-body text-fg-muted">Live on-hand buckets by product, location, and status.</p>
      </div>
      <div className="grid grid-cols-3 gap-4">
        <Field label="Location ID">
          {({ id }) => (
            <Input id={id} onChange={(e) => dispatch(setQuery({ locationId: e.target.value || undefined }))} />
          )}
        </Field>
        <Field label="Product ID">
          {({ id }) => (
            <Input id={id} onChange={(e) => dispatch(setQuery({ productId: e.target.value || undefined }))} />
          )}
        </Field>
        <Field label="Variant ID">
          {({ id }) => (
            <Input id={id} onChange={(e) => dispatch(setQuery({ variantId: e.target.value || undefined }))} />
          )}
        </Field>
      </div>
      <div className="overflow-x-auto rounded-lg border border-border">
        <table className="w-full border-collapse text-body">
          <thead>
            <tr className="bg-surface-muted">
              {["Product ID", "Variant ID", "Location ID", "Status", "Qty on hand", "Unit cost", "Total value"].map(
                (h) => (
                  <th
                    key={h}
                    className="border-b border-border px-4 py-2.5 text-left text-small font-semibold uppercase tracking-wide text-fg-muted"
                  >
                    {h}
                  </th>
                ),
              )}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={7} className="px-4 py-10 text-center text-fg-muted">
                  <Loader2 className="mx-auto h-5 w-5 animate-spin" aria-label="Loading" />
                </td>
              </tr>
            ) : rows.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-4 py-10 text-center text-fg-muted">
                  No stock records found.
                </td>
              </tr>
            ) : (
              rows.map((r, idx) => (
                <tr key={`${r.productId}-${r.variantId}-${r.locationId}-${r.status}-${idx}`} className="hover:bg-surface-muted">
                  <td className="border-b border-border px-4 py-3 text-fg">{r.productId}</td>
                  <td className="border-b border-border px-4 py-3 text-fg">{r.variantId ?? "—"}</td>
                  <td className="border-b border-border px-4 py-3 text-fg">{r.locationId}</td>
                  <td className="border-b border-border px-4 py-3">
                    <StatusPill label={r.status} tone={STATUS_TONE[r.status]} />
                  </td>
                  <td className="border-b border-border px-4 py-3 text-right tabular-nums text-fg">{r.qtyOnHand}</td>
                  <td className="border-b border-border px-4 py-3 text-right tabular-nums text-fg">
                    {r.unitCostCurrency ? formatMoney({ amountMinor: r.unitCostAmount, currency: r.unitCostCurrency }) : "—"}
                  </td>
                  <td className="border-b border-border px-4 py-3 text-right tabular-nums text-fg">
                    {r.unitCostCurrency ? formatMoney({ amountMinor: r.totalValue, currency: r.unitCostCurrency }) : "—"}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

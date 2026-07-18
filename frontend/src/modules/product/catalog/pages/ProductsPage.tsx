import { useEffect, useState } from "react";
import { Plus, Pencil, Trash2 } from "lucide-react";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { usePermission } from "@/shared/hooks/usePermission";
import { PERMISSIONS } from "@/shared/constants/permissionCodes";
import { DataTable, type Column } from "@/shared/components/DataTable";
import { StatusPill } from "@/shared/components/StatusPill";
import { Button } from "@/shared/components/Button";
import { formatMoney } from "@/shared/utils/format";
import { fetchProducts, setQuery, deleteProduct } from "../slice/productSlice";
import { ProductFormModal } from "../components/ProductFormModal";
import type { Product, LifecycleState } from "../api/productApi";

const LIFECYCLE_TONE: Record<LifecycleState, "success" | "neutral" | "warning" | "danger"> = {
  DRAFT: "neutral",
  ACTIVE: "success",
  ON_HOLD: "warning",
  RUN_OUT: "warning",
  DISCONTINUED: "danger",
};

/** Products list + CRUD (composition only — ARCHITECTURE.md §6 ≤150). */
export default function ProductsPage() {
  const dispatch = useAppDispatch();
  const { has } = usePermission();
  const canWrite = has(PERMISSIONS.PRODUCT_WRITE);
  const { page, query, loading } = useAppSelector((s) => s.product);
  const [editing, setEditing] = useState<Product | null>(null);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    void dispatch(fetchProducts(query));
  }, [dispatch, query]);

  const openCreate = () => {
    setEditing(null);
    setOpen(true);
  };

  const columns: Column<Product>[] = [
    { key: "sku", header: "SKU", render: (p) => <span className="font-semibold">{p.sku}</span> },
    { key: "name", header: "Name", render: (p) => p.name },
    { key: "brand", header: "Brand", render: (p) => p.brand ?? "—" },
    {
      key: "sell",
      header: "Sell price",
      align: "right",
      render: (p) => formatMoney({ amountMinor: p.sellAmount, currency: p.sellCurrency }),
    },
    {
      key: "lifecycle",
      header: "Lifecycle",
      render: (p) => <StatusPill label={p.lifecycleState} tone={LIFECYCLE_TONE[p.lifecycleState]} />,
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (p) =>
        canWrite ? (
          <div className="flex justify-end gap-1">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                setEditing(p);
                setOpen(true);
              }}
              aria-label="Edit"
            >
              <Pencil className="h-4 w-4" />
            </Button>
            <Button variant="ghost" size="sm" onClick={() => dispatch(deleteProduct(p.id))} aria-label="Delete">
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
          <h1 className="text-h1 text-fg">Products</h1>
          <p className="mt-1 text-body text-fg-muted">Product master — catalog, pricing, and lifecycle.</p>
        </div>
        {canWrite && (
          <Button onClick={openCreate}>
            <Plus className="h-4 w-4" />
            New product
          </Button>
        )}
      </div>
      <DataTable
        columns={columns}
        page={page}
        loading={loading}
        rowKey={(p) => p.id}
        onPageChange={(pg) => dispatch(setQuery({ page: pg }))}
        emptyMessage="No products yet."
      />
      <ProductFormModal open={open} onOpenChange={setOpen} editing={editing} />
    </div>
  );
}

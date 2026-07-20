import { useCallback } from "react";
import { http } from "@/shared/services/http";
import { EntitySelect, type EntityOption } from "./EntitySelect";

/** Accept either a PageResponse<T> or a bare T[] and return the rows. */
function rows<T>(data: unknown): T[] {
  if (Array.isArray(data)) return data as T[];
  const page = data as { content?: T[] } | null;
  return page?.content ?? [];
}

interface PickerProps {
  value: string;
  onChange: (value: string, option: EntityOption | null) => void;
  selectedLabel?: string;
  disabled?: boolean;
  invalid?: boolean;
  placeholder?: string;
}

/** Supplier picker — GET /procurement/suppliers?search= (active by default). */
export function SupplierPicker(props: PickerProps) {
  const fetchOptions = useCallback(async (q: string) => {
    const data = await http
      .get("/procurement/suppliers", { params: { search: q || undefined, size: 20 } })
      .then((r) => r.data);
    return rows<{ id: string; code: string; name: string }>(data).map((s) => ({
      value: s.id,
      label: `${s.code} — ${s.name}`,
      meta: s,
    }));
  }, []);
  return <EntitySelect {...props} fetchOptions={fetchOptions} placeholder={props.placeholder ?? "Search suppliers…"} />;
}

/** Product picker — GET /products?q= */
export function ProductPicker(props: PickerProps) {
  const fetchOptions = useCallback(async (q: string) => {
    const data = await http.get("/products", { params: { q: q || undefined, size: 20 } }).then((r) => r.data);
    return rows<{ id: string; sku: string; name: string }>(data).map((p) => ({
      value: p.id,
      label: `${p.sku} — ${p.name}`,
      meta: p,
    }));
  }, []);
  return <EntitySelect {...props} fetchOptions={fetchOptions} placeholder={props.placeholder ?? "Search products…"} />;
}

/** GL account picker — GET /finance/accounts?companyId=&q= */
export function AccountPicker({ companyId, ...props }: PickerProps & { companyId: string }) {
  const fetchOptions = useCallback(
    async (q: string) => {
      if (!companyId) return [];
      const data = await http
        .get("/finance/accounts", { params: { companyId, q: q || undefined, size: 50 } })
        .then((r) => r.data);
      return rows<{ id: string; code: string; name: string; postingType?: string }>(data).map((a) => ({
        value: a.id,
        label: `${a.code} — ${a.name}`,
        hint: a.postingType === "HEADER" ? "Group" : undefined,
        meta: a,
      }));
    },
    [companyId],
  );
  return <EntitySelect {...props} fetchOptions={fetchOptions} placeholder={props.placeholder ?? "Search accounts…"} />;
}

/** Location picker — GET /locations?q= */
export function LocationPicker(props: PickerProps) {
  const fetchOptions = useCallback(async (q: string) => {
    const data = await http.get("/locations", { params: { q: q || undefined, size: 50 } }).then((r) => r.data);
    return rows<{ id: string; code: string; name: string }>(data).map((l) => ({
      value: l.id,
      label: `${l.code} — ${l.name}`,
      meta: l,
    }));
  }, []);
  return <EntitySelect {...props} fetchOptions={fetchOptions} placeholder={props.placeholder ?? "Search locations…"} />;
}

/** Currency picker — GET /currencies?q=&active=true (value = ISO code). */
export function CurrencyPicker(props: PickerProps) {
  const fetchOptions = useCallback(async (q: string) => {
    const data = await http
      .get("/currencies", { params: { q: q || undefined, active: true, size: 50 } })
      .then((r) => r.data);
    return rows<{ code: string; name: string; symbol?: string }>(data).map((c) => ({
      value: c.code,
      label: `${c.code} — ${c.name}`,
      meta: c,
    }));
  }, []);
  return <EntitySelect {...props} fetchOptions={fetchOptions} placeholder={props.placeholder ?? "Search currencies…"} />;
}

/** Tax-code picker — GET /tax-codes (filtered client-side). */
export function TaxCodePicker(props: PickerProps) {
  const fetchOptions = useCallback(async (q: string) => {
    const data = await http.get("/tax-codes", { params: { size: 100 } }).then((r) => r.data);
    const term = q.trim().toLowerCase();
    return rows<{ id: string; code: string; ratePercent?: number; description?: string }>(data)
      .filter((t) => !term || t.code.toLowerCase().includes(term) || (t.description ?? "").toLowerCase().includes(term))
      .map((t) => ({
        value: t.id,
        label: `${t.code}${t.ratePercent != null ? ` (${t.ratePercent}%)` : ""}`,
        hint: t.description,
        meta: t,
      }));
  }, []);
  return <EntitySelect {...props} fetchOptions={fetchOptions} placeholder={props.placeholder ?? "Search tax codes…"} />;
}

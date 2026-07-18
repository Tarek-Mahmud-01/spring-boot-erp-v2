import type { ReactNode } from "react";
import { Loader2 } from "lucide-react";
import { cn } from "@/shared/utils/cn";
import type { Paginated } from "@/shared/types/api";
import { Pagination } from "./Pagination";

export interface Column<T> {
  key: string;
  header: string;
  render: (row: T) => ReactNode;
  align?: "left" | "right" | "center";
  className?: string;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  page: Paginated<T> | null;
  loading?: boolean;
  rowKey: (row: T) => string;
  onPageChange: (page: number) => void;
  emptyMessage?: string;
}

/**
 * Server-driven, presentational data table (ARCHITECTURE.md §3.2). Renders one
 * page of already-joined DTOs; never filters or slices client-side. The
 * container owns the query (page/size/sort/filter) and refetches on change.
 */
export function DataTable<T>({
  columns,
  page,
  loading = false,
  rowKey,
  onPageChange,
  emptyMessage = "No records found.",
}: DataTableProps<T>) {
  const rows = page?.content ?? [];

  return (
    <div className="flex flex-col gap-2">
      <div className="overflow-x-auto rounded-lg border border-border">
        <table className="w-full border-collapse text-body">
          <thead>
            <tr className="bg-surface-muted">
              {columns.map((col) => (
                <th
                  key={col.key}
                  className={cn(
                    "border-b border-border px-4 py-2.5 text-small font-semibold uppercase tracking-wide text-fg-muted",
                    col.align === "right" && "text-right",
                    col.align === "center" && "text-center",
                    !col.align && "text-left",
                  )}
                >
                  {col.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={columns.length} className="px-4 py-10 text-center text-fg-muted">
                  <Loader2 className="mx-auto h-5 w-5 animate-spin" aria-label="Loading" />
                </td>
              </tr>
            ) : rows.length === 0 ? (
              <tr>
                <td colSpan={columns.length} className="px-4 py-10 text-center text-fg-muted">
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              rows.map((row) => (
                <tr key={rowKey(row)} className="hover:bg-surface-muted">
                  {columns.map((col) => (
                    <td
                      key={col.key}
                      className={cn(
                        "border-b border-border px-4 py-3 text-fg",
                        col.align === "right" && "text-right tabular-nums",
                        col.align === "center" && "text-center",
                        col.className,
                      )}
                    >
                      {col.render(row)}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
      {page && (
        <Pagination
          page={page.page}
          size={page.size}
          totalElements={page.totalElements}
          totalPages={page.totalPages}
          onPageChange={onPageChange}
        />
      )}
    </div>
  );
}

import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "./Button";

interface PaginationProps {
  page: number; // zero-based
  size: number;
  totalElements: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

/**
 * Server-driven pagination control (ARCHITECTURE.md §3.2). Purely
 * presentational — reports the requested page; the container refetches.
 */
export function Pagination({ page, size, totalElements, totalPages, onPageChange }: PaginationProps) {
  const from = totalElements === 0 ? 0 : page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);

  return (
    <div className="flex items-center justify-between gap-4 py-2 text-small text-fg-muted">
      <span>
        Showing {from}–{to} of {totalElements}
      </span>
      <div className="flex items-center gap-2">
        <Button
          variant="secondary"
          size="sm"
          onClick={() => onPageChange(page - 1)}
          disabled={page <= 0}
          aria-label="Previous page"
        >
          <ChevronLeft className="h-4 w-4" />
          Prev
        </Button>
        <span className="tabular-nums">
          {totalPages === 0 ? 0 : page + 1} / {totalPages}
        </span>
        <Button
          variant="secondary"
          size="sm"
          onClick={() => onPageChange(page + 1)}
          disabled={page + 1 >= totalPages}
          aria-label="Next page"
        >
          Next
          <ChevronRight className="h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}

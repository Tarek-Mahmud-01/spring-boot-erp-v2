import { Fragment, type ReactNode } from "react";
import { ChevronRight } from "lucide-react";
import { cn } from "@/shared/utils/cn";

export interface Crumb {
  label: string;
  to?: string;
}

interface PageHeaderProps {
  title: string;
  subtitle?: string;
  breadcrumbs?: Crumb[];
  actions?: ReactNode;
  className?: string;
}

/** Standard page header: breadcrumbs + title + subtitle + right-aligned actions slot. */
export function PageHeader({ title, subtitle, breadcrumbs, actions, className }: PageHeaderProps) {
  return (
    <div className={cn("flex flex-col gap-3", className)}>
      {breadcrumbs && breadcrumbs.length > 0 && (
        <nav className="flex items-center gap-1 text-small text-fg-muted" aria-label="Breadcrumb">
          {breadcrumbs.map((c, i) => (
            <Fragment key={`${c.label}-${i}`}>
              {i > 0 && <ChevronRight className="h-3.5 w-3.5" aria-hidden />}
              <span className={cn(i === breadcrumbs.length - 1 && "text-fg")}>{c.label}</span>
            </Fragment>
          ))}
        </nav>
      )}
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-h1 text-fg">{title}</h1>
          {subtitle && <p className="mt-1 text-body text-fg-muted">{subtitle}</p>}
        </div>
        {actions && <div className="flex shrink-0 items-center gap-2">{actions}</div>}
      </div>
    </div>
  );
}

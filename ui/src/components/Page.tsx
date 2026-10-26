import type { ReactNode } from "react";
import { Link } from "react-router";
import { CaretRight } from "@phosphor-icons/react";

/** The content column every page uses: contained, with responsive gutters. */
export function Page({ children }: { children: ReactNode }) {
  return <div className="mx-auto flex w-full max-w-[1400px] flex-col gap-6 px-4 py-6 sm:px-6 lg:px-8 lg:py-8">{children}</div>;
}

export type Crumb = { label: string; to?: string };

/** Breadcrumb, title, meta line and actions. Left-aligned; actions wrap under the title on small screens. */
export function PageHeader({ crumbs, title, meta, actions }: { crumbs?: Crumb[]; title: ReactNode; meta?: ReactNode; actions?: ReactNode }) {
  return (
    <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div className="flex min-w-0 flex-col gap-2">
        {crumbs && crumbs.length > 0 && (
          <nav aria-label="Breadcrumb">
            <ol className="flex flex-wrap items-center gap-1 text-sm text-fg-subtle">
              {crumbs.map((crumb, index) => (
                <li key={`${crumb.label}-${index}`} className="flex items-center gap-1">
                  {crumb.to ? (
                    <Link to={crumb.to} className="focus-ring rounded-sm hover:text-fg">
                      {crumb.label}
                    </Link>
                  ) : (
                    <span aria-current="page">{crumb.label}</span>
                  )}
                  {index < crumbs.length - 1 && <CaretRight aria-hidden size={12} />}
                </li>
              ))}
            </ol>
          </nav>
        )}
        <h1 className="text-2xl font-semibold tracking-tight break-words text-fg">{title}</h1>
        {meta && <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-fg-muted">{meta}</div>}
      </div>
      {actions && <div className="flex shrink-0 flex-wrap gap-2">{actions}</div>}
    </header>
  );
}

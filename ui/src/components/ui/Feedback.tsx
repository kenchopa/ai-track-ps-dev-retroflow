import { CheckCircle, Info, WarningCircle, WarningOctagon } from "@phosphor-icons/react";
import type { ReactNode } from "react";
import { cx } from "../../lib/cx";

/** A placeholder in the shape of the content it replaces (no spinners). */
export function Skeleton({ className }: { className?: string }) {
  return <div aria-hidden className={cx("skeleton-shimmer rounded-md bg-surface-2", className)} />;
}

/** Wraps skeletons so assistive technology hears one "Loading" instead of nothing. */
export function LoadingRegion({ label, children, className }: { label: string; children: ReactNode; className?: string }) {
  return (
    <div role="status" aria-busy="true" className={className}>
      <span className="sr-only">{label}</span>
      {children}
    </div>
  );
}

type EmptyProps = { icon: ReactNode; title: string; description?: ReactNode; action?: ReactNode; className?: string };

/** What this place is for, and the one action that fills it. */
export function EmptyState({ icon, title, description, action, className }: EmptyProps) {
  return (
    <div className={cx("flex flex-col items-start gap-3 rounded-lg border border-dashed border-line-strong px-5 py-6", className)}>
      <span aria-hidden className="inline-flex h-9 w-9 items-center justify-center rounded-md bg-surface-2 text-fg-muted">
        {icon}
      </span>
      <div className="flex flex-col gap-1">
        <p className="text-sm font-medium text-fg">{title}</p>
        {description && <p className="max-w-[60ch] text-sm leading-relaxed text-fg-muted">{description}</p>}
      </div>
      {action}
    </div>
  );
}

type AlertTone = "info" | "success" | "warning" | "danger";

const alertTones: Record<AlertTone, { box: string; icon: ReactNode }> = {
  info: { box: "border-line bg-surface-2 text-fg", icon: <Info className="text-fg-muted" /> },
  success: { box: "border-positive/30 bg-positive-soft text-fg", icon: <CheckCircle className="text-positive" /> },
  warning: { box: "border-attention/30 bg-attention-soft text-fg", icon: <WarningCircle className="text-attention" /> },
  danger: { box: "border-danger/30 bg-danger-soft text-fg", icon: <WarningOctagon className="text-danger" /> },
};

type AlertProps = { tone?: AlertTone; title?: string; children?: ReactNode; action?: ReactNode; className?: string };

/** Inline, in-place explanation. Danger alerts interrupt screen readers, the others are polite. */
export function InlineAlert({ tone = "info", title, children, action, className }: AlertProps) {
  const { box, icon } = alertTones[tone];
  return (
    <div role={tone === "danger" ? "alert" : "status"} className={cx("flex gap-3 rounded-lg border px-4 py-3 text-sm", box, className)}>
      <span aria-hidden className="mt-0.5 shrink-0">
        {icon}
      </span>
      <div className="flex min-w-0 flex-1 flex-col gap-1">
        {title && <p className="font-medium">{title}</p>}
        {children && <div className="leading-relaxed text-fg-muted">{children}</div>}
        {action && <div className="mt-1">{action}</div>}
      </div>
    </div>
  );
}

import type { ReactNode } from "react";
import type { Priority } from "../../api/types";
import { cx } from "../../lib/cx";

export type Tone = "neutral" | "positive" | "attention" | "accent" | "danger";

const tones: Record<Tone, string> = {
  neutral: "bg-surface-2 text-fg-muted",
  positive: "bg-positive-soft text-positive",
  attention: "bg-attention-soft text-attention",
  accent: "bg-accent-soft text-accent",
  danger: "bg-danger-soft text-danger",
};

type Props = { tone?: Tone; mono?: boolean; className?: string; children: ReactNode };

export function Badge({ tone = "neutral", mono = false, className, children }: Props) {
  return (
    <span
      className={cx(
        "inline-flex h-5 shrink-0 items-center gap-1 rounded-sm px-1.5 text-xs font-medium",
        mono && "font-mono tracking-wide uppercase",
        tones[tone],
        className,
      )}
    >
      {children}
    </span>
  );
}

const priorityTone: Record<Priority, Tone> = { HIGH: "danger", MEDIUM: "attention", LOW: "neutral" };

export function PriorityBadge({ priority }: { priority: Priority }) {
  return (
    <Badge tone={priorityTone[priority]} mono>
      <span className="sr-only">Priority </span>
      {priority.toLowerCase()}
    </Badge>
  );
}

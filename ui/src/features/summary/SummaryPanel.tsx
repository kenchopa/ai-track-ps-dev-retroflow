import { ArrowClockwise, ListChecks, Sparkle, ThumbsUp, Wrench } from "@phosphor-icons/react";
import { motion } from "framer-motion";
import { useEffect, useState, type ReactNode } from "react";
import { ApiError, messageOf } from "../../api/errors";
import { useSummary } from "../../api/hooks";
import type { Retrospective, Summary } from "../../api/types";
import { Badge, PriorityBadge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { InlineAlert, LoadingRegion, Skeleton } from "../../components/ui/Feedback";
import { cx } from "../../lib/cx";
import { itemVariants, listVariants } from "../../lib/motion";

type Failure = { tone: "info" | "warning" | "danger"; title: string; detail: string; canRetry: boolean };

/** Every way the summary endpoint can fail, as a title, a next step and whether retrying helps. */
export function describeFailure(error: unknown): Failure {
  if (!(error instanceof ApiError) || error.status === 0) {
    return { tone: "danger", title: "RetroFlow can't be reached", detail: messageOf(error), canRetry: true };
  }
  switch (error.status) {
    case 503:
      if (error.retryAfter) {
        return {
          tone: "warning",
          title: "The summary service is busy",
          detail: `Too many summaries were asked for at once. Try again in ${error.retryAfter} seconds.`,
          canRetry: true,
        };
      }
      return {
        tone: "info",
        title: "Summaries aren't set up on this server",
        detail: "An administrator has to configure the OpenAI key (OPENAI_API_KEY) before RetroFlow can write summaries.",
        canRetry: false,
      };
    case 504:
      return { tone: "warning", title: "The summary took too long", detail: "The model didn't answer in time. Trying again usually works.", canRetry: true };
    case 502:
      return {
        tone: "warning",
        title: "The summary couldn't be used",
        detail: `${error.message}. RetroFlow checks every summary against the feedback and rejected this one.`,
        canRetry: true,
      };
    default:
      return { tone: "danger", title: "No summary for this retrospective", detail: error.message, canRetry: false };
  }
}

/**
 * Only for a CLOSED retrospective (the API's rule). Says where the feedback goes before the
 * first click, shows the summary in its final layout while it is being written, and explains failures.
 */
export function SummaryPanel({ retro, itemCount }: { retro: Retrospective; itemCount: number }) {
  const summary = useSummary(retro.id);
  const [cooldown, setCooldown] = useState(0);

  useEffect(() => {
    if (!(summary.error instanceof ApiError) || !summary.error.retryAfter) return;
    setCooldown(summary.error.retryAfter);
    const timer = window.setInterval(() => setCooldown((seconds) => (seconds <= 1 ? 0 : seconds - 1)), 1000);
    return () => window.clearInterval(timer);
  }, [summary.error]);

  const failure = summary.isError ? describeFailure(summary.error) : null;

  return (
    <section aria-labelledby="summary-heading" className="flex flex-col gap-4 rounded-xl border border-line bg-surface p-5 shadow-sm sm:p-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex flex-col gap-1">
          <h2 id="summary-heading" className="flex items-center gap-2 text-base font-semibold tracking-tight text-fg">
            <Sparkle aria-hidden className="text-accent" />
            Summary
            <Badge tone="neutral">AI</Badge>
          </h2>
          {!summary.data && (
            <p className="max-w-[65ch] text-sm leading-relaxed text-fg-muted">
              A short summary of what went well, what needs to improve and the open action items. The feedback text is sent to OpenAI to
              write it; author names are not.
            </p>
          )}
        </div>
        {summary.data ? (
          <Button size="sm" variant="ghost" icon={<ArrowClockwise size={16} />} loading={summary.isPending} onClick={() => summary.mutate()}>
            Write again
          </Button>
        ) : (
          !summary.isPending && (
            <Button
              variant="primary"
              icon={<Sparkle />}
              onClick={() => summary.mutate()}
              disabled={itemCount === 0 || cooldown > 0 || (failure !== null && !failure.canRetry)}
            >
              {failure?.canRetry ? (cooldown > 0 ? `Try again in ${cooldown}s` : "Try again") : "Summarize"}
            </Button>
          )
        )}
      </div>

      {itemCount === 0 && !summary.data && <p className="text-sm text-fg-subtle">This retrospective has no feedback to summarize.</p>}

      {summary.isPending && <SummarySkeleton />}

      {failure && !summary.isPending && (
        <InlineAlert tone={failure.tone} title={failure.title}>
          {failure.detail}
        </InlineAlert>
      )}

      {summary.data && !summary.isPending && <SummaryResult summary={summary.data} />}
    </section>
  );
}

function SummarySkeleton() {
  return (
    <LoadingRegion label="Writing the summary" className="grid grid-cols-1 gap-6 md:grid-cols-3">
      {[0, 1, 2].map((column) => (
        <div key={column} className="flex flex-col gap-2.5">
          <Skeleton className="h-4 w-28" />
          <Skeleton className="h-3.5 w-full" />
          <Skeleton className="h-3.5 w-11/12" />
          <Skeleton className="h-3.5 w-3/4" />
        </div>
      ))}
    </LoadingRegion>
  );
}

function SummaryResult({ summary }: { summary: Summary }) {
  return (
    <div className="flex flex-col gap-4">
      <motion.div variants={listVariants} initial="hidden" animate="show" className="grid grid-cols-1 gap-6 md:grid-cols-3">
        <SummarySection title="Went well" icon={<ThumbsUp size={16} />} marker="text-positive" empty="Nothing in this section.">
          {summary.wentWell.map((point) => (
            <li key={point}>{point}</li>
          ))}
        </SummarySection>
        <SummarySection title="Needs improvement" icon={<Wrench size={16} />} marker="text-attention" empty="Nothing in this section.">
          {summary.needsImprovement.map((point) => (
            <li key={point}>{point}</li>
          ))}
        </SummarySection>
        <SummarySection title="Open action items" icon={<ListChecks size={16} />} marker="text-accent" empty="Every action item is done.">
          {summary.openActionItems.map((item) => (
            <li key={item.id} className="flex items-start gap-2">
              <PriorityBadge priority={item.priority} />
              <span>{item.summary}</span>
            </li>
          ))}
        </SummarySection>
      </motion.div>
      <p className="border-t border-line pt-3 text-xs text-fg-subtle">Written by an AI model and checked against the feedback. Read it before you share it.</p>
    </div>
  );
}

function SummarySection({ title, icon, marker, empty, children }: { title: string; icon: ReactNode; marker: string; empty: string; children: ReactNode[] }) {
  return (
    <motion.section variants={itemVariants} className="flex flex-col gap-2">
      <h3 className="flex items-center gap-1.5 text-sm font-semibold text-fg">
        <span aria-hidden className={cx(marker)}>
          {icon}
        </span>
        {title}
      </h3>
      {children.length === 0 ? (
        <p className="text-sm text-fg-subtle">{empty}</p>
      ) : (
        <ul className="flex list-none flex-col gap-2 text-sm leading-relaxed text-fg-muted">{children}</ul>
      )}
    </motion.section>
  );
}

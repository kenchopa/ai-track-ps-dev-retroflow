import { ArrowRight, ListChecks } from "@phosphor-icons/react";
import { AnimatePresence, motion } from "framer-motion";
import { useMemo, useState } from "react";
import { Link } from "react-router";
import { messageOf } from "../../api/errors";
import { useRetrospectives, useTeamActionItems } from "../../api/hooks";
import type { ActionItem, ActionItemFilter, Priority, Team } from "../../api/types";
import { Avatar } from "../../components/ui/Avatar";
import { PriorityBadge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { EmptyState, InlineAlert, LoadingRegion, Skeleton } from "../../components/ui/Feedback";
import { SegmentedControl } from "../../components/ui/SegmentedControl";
import { cx } from "../../lib/cx";
import { itemVariants, listVariants, spring } from "../../lib/motion";
import { CompleteControl } from "./CompleteControl";

type Status = "open" | "done" | "all";
type PriorityFilter = "ALL" | Priority;

const RANK: Record<Priority, number> = { HIGH: 0, MEDIUM: 1, LOW: 2 };

function toFilter(status: Status, priority: PriorityFilter): ActionItemFilter {
  return {
    completed: status === "all" ? undefined : status === "done",
    priority: priority === "ALL" ? undefined : priority,
  };
}

/** Open first, then by priority, newest first within a priority. */
export function sortActionItems(items: ActionItem[]): ActionItem[] {
  return [...items].sort((a, b) => Number(a.completed) - Number(b.completed) || RANK[a.priority] - RANK[b.priority] || b.id - a.id);
}

function emptyText(status: Status, priority: PriorityFilter): string {
  const level = priority === "ALL" ? "" : `${priority.toLowerCase()}-priority `;
  if (status === "open") return `No open ${level}action items. Everything agreed on is done.`;
  if (status === "done") return `No ${level}action items are done yet.`;
  return `No ${level}action items yet. They are added during a retrospective.`;
}

/**
 * Every action item of a team, across all its retrospectives (decision A3).
 * `compact` is the preview next to the retrospective list: open items only, the first five.
 */
export function ActionItemsPanel({ team, compact = false }: { team: Team; compact?: boolean }) {
  const [status, setStatus] = useState<Status>("open");
  const [priority, setPriority] = useState<PriorityFilter>("ALL");
  const filter = useMemo(() => toFilter(compact ? "open" : status, compact ? "ALL" : priority), [compact, status, priority]);
  const items = useTeamActionItems(team.id, filter);
  const retros = useRetrospectives(team.id);
  const titles = useMemo(() => new Map(retros.data?.map((retro) => [retro.id, retro.title])), [retros.data]);
  const sorted = useMemo(() => sortActionItems(items.data ?? []), [items.data]);
  const shown = compact ? sorted.slice(0, 5) : sorted;

  return (
    <section aria-labelledby={`actions-${team.id}`} className="flex flex-col gap-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <h2 id={`actions-${team.id}`} className={cx("font-semibold tracking-tight text-fg", compact ? "text-base" : "text-lg")}>
          {compact ? "Open action items" : "Action items"}
          {compact && items.data && <span className="ml-2 font-mono text-sm font-normal text-fg-subtle tabular">{items.data.length}</span>}
        </h2>
        {!compact && (
          <div className="flex flex-wrap gap-3">
            <SegmentedControl<Status>
              label="Status"
              value={status}
              onChange={setStatus}
              options={[
                { value: "open", label: "Open" },
                { value: "done", label: "Done" },
                { value: "all", label: "All" },
              ]}
            />
            <SegmentedControl<PriorityFilter>
              label="Priority"
              value={priority}
              onChange={setPriority}
              options={[
                { value: "ALL", label: "Any priority" },
                { value: "HIGH", label: "High" },
                { value: "MEDIUM", label: "Medium" },
                { value: "LOW", label: "Low" },
              ]}
            />
          </div>
        )}
      </div>

      {items.isPending && (
        <LoadingRegion label="Loading action items" className="flex flex-col gap-2">
          {[0, 1, 2].map((row) => (
            <Skeleton key={row} className="h-16 rounded-lg" />
          ))}
        </LoadingRegion>
      )}

      {items.isError && (
        <InlineAlert tone="danger" title="Action items couldn't be loaded" action={<Button size="sm" onClick={() => items.refetch()}>Try again</Button>}>
          {messageOf(items.error)}
        </InlineAlert>
      )}

      {items.data && sorted.length === 0 && (
        <EmptyState icon={<ListChecks />} title={compact ? "Nothing open" : "No action items"} description={emptyText(compact ? "open" : status, compact ? "ALL" : priority)} />
      )}

      {shown.length > 0 && (
        <motion.ul variants={listVariants} initial="hidden" animate="show" className="flex flex-col divide-y divide-line overflow-hidden rounded-lg border border-line bg-surface shadow-sm">
          <AnimatePresence initial={false}>
            {shown.map((item) => (
              <motion.li key={item.id} layout="position" variants={itemVariants} exit="exit" transition={spring} className="flex items-start gap-3 px-3 py-3 sm:px-4">
                <CompleteControl item={item} teamId={team.id} />
                <div className="flex min-w-0 flex-1 flex-col gap-1.5 pt-1.5">
                  <p className={cx("text-sm leading-relaxed break-words", item.completed ? "text-fg-subtle" : "text-fg")}>{item.content}</p>
                  <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-fg-subtle">
                    <PriorityBadge priority={item.priority} />
                    <Link to={`/retrospectives/${item.retrospectiveId}`} className="focus-ring rounded-sm hover:text-fg">
                      {titles.get(item.retrospectiveId) ?? "Retrospective"}
                    </Link>
                    <span className="flex items-center gap-1.5">
                      <Avatar name={item.author} decorative />
                      {item.author}
                    </span>
                  </div>
                </div>
              </motion.li>
            ))}
          </AnimatePresence>
        </motion.ul>
      )}

      {compact && sorted.length > shown.length && (
        <Link to={`/teams/${team.id}/action-items`} className="focus-ring inline-flex items-center gap-1.5 self-start rounded-sm text-sm font-medium text-accent hover:text-accent-strong">
          See all {sorted.length} open action items
          <ArrowRight aria-hidden size={14} />
        </Link>
      )}
    </section>
  );
}

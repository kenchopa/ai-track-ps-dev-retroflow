import { LockSimple } from "@phosphor-icons/react";
import { AnimatePresence, motion } from "framer-motion";
import { useState } from "react";
import { messageOf } from "../../api/errors";
import { useFeedbackItems } from "../../api/hooks";
import { isActionItem, type FeedbackItem, type FeedbackType, type Retrospective } from "../../api/types";
import { Button } from "../../components/ui/Button";
import { InlineAlert, LoadingRegion, Skeleton } from "../../components/ui/Feedback";
import { Tabs, panelId, tabId } from "../../components/ui/Tabs";
import { cx } from "../../lib/cx";
import { listVariants } from "../../lib/motion";
import { useMediaQuery } from "../../lib/useMediaQuery";
import { sortActionItems } from "../actions/ActionItemsPanel";
import { COLUMNS, type Column } from "./columns";
import { Composer } from "./Composer";
import { ItemCard } from "./ItemCard";

type Props = { retro: Retrospective; author: string };

/**
 * The three kinds of feedback side by side (md and up) or as tabs (small screens).
 * Three equal columns here are functional parity, not a decorative card row.
 */
export function RetroBoard({ retro, author }: Props) {
  const open = retro.status === "OPEN";
  const items = useFeedbackItems(retro.id, open);
  const [tab, setTab] = useState<FeedbackType>("WENT_WELL");
  const wide = useMediaQuery("(min-width: 768px)");

  if (items.isPending) {
    return (
      <LoadingRegion label="Loading feedback" className="grid grid-cols-1 gap-4 md:grid-cols-3">
        {COLUMNS.map((column) => (
          <div key={column.type} className="flex flex-col gap-3">
            <Skeleton className="h-6 w-32" />
            <Skeleton className="h-24 rounded-lg" />
            <Skeleton className="h-16 rounded-lg" />
          </div>
        ))}
      </LoadingRegion>
    );
  }

  if (items.isError) {
    return (
      <InlineAlert tone="danger" title="The feedback couldn't be loaded" action={<Button size="sm" onClick={() => items.refetch()}>Try again</Button>}>
        {messageOf(items.error)}
      </InlineAlert>
    );
  }

  const byType = (type: FeedbackType): FeedbackItem[] => {
    const ofType = items.data.filter((item) => item.type === type);
    return type === "ACTION_ITEM" ? sortActionItems(ofType.filter(isActionItem)) : ofType;
  };

  if (wide) {
    return (
      <div className="grid grid-cols-3 gap-4 xl:gap-6">
        {COLUMNS.map((column) => (
          <BoardColumn key={column.type} column={column} items={byType(column.type)} retro={retro} author={author} />
        ))}
      </div>
    );
  }

  const current = COLUMNS.find((column) => column.type === tab) ?? COLUMNS[0]!;
  return (
    <div className="flex flex-col gap-4">
      <Tabs
        label="Feedback"
        idPrefix="board"
        value={tab}
        onChange={setTab}
        tabs={COLUMNS.map((column) => ({ value: column.type, label: column.short, count: byType(column.type).length }))}
        fill
      />
      <BoardColumn key={current.type} column={current} items={byType(current.type)} retro={retro} author={author} asTabPanel />
    </div>
  );
}

type ColumnProps = { column: Column; items: FeedbackItem[]; retro: Retrospective; author: string; asTabPanel?: boolean };

function BoardColumn({ column, items, retro, author, asTabPanel = false }: ColumnProps) {
  const open = retro.status === "OPEN";
  const headingId = `column-${column.type}`;
  return (
    <section
      id={asTabPanel ? panelId("board", column.type) : undefined}
      role={asTabPanel ? "tabpanel" : undefined}
      aria-labelledby={asTabPanel ? tabId("board", column.type) : headingId}
      className="flex min-w-0 flex-col gap-3"
    >
      <h2 id={headingId} className={cx("flex items-center gap-2 text-sm font-semibold text-fg", asTabPanel && "sr-only")}>
        <span aria-hidden className={cx("inline-flex h-7 w-7 items-center justify-center rounded-md", column.marker)}>
          {column.icon}
        </span>
        {column.title}
        <span className="font-mono text-xs font-normal text-fg-subtle tabular">{items.length}</span>
        {!open && (
          <span className="ml-auto inline-flex items-center gap-1 text-xs font-normal text-fg-subtle" title="The retrospective is closed">
            <LockSimple size={14} aria-hidden />
            Read-only
          </span>
        )}
      </h2>

      {open && <Composer retro={retro} column={column} author={author} />}

      {items.length === 0 ? (
        <p className="rounded-lg border border-dashed border-line-strong px-4 py-5 text-sm leading-relaxed text-fg-subtle">
          {open ? column.emptyOpen : column.emptyClosed}
        </p>
      ) : (
        <motion.ul variants={listVariants} initial="hidden" animate="show" className="flex flex-col gap-2" aria-label={column.title}>
          <AnimatePresence initial={false}>
            {items.map((item) => (
              <ItemCard key={item.id} item={item} retro={retro} />
            ))}
          </AnimatePresence>
        </motion.ul>
      )}
    </section>
  );
}

import { PencilSimple, Trash } from "@phosphor-icons/react";
import { motion } from "framer-motion";
import { useState, type KeyboardEvent } from "react";
import { messageOf } from "../../api/errors";
import { useDeleteItem, useUpdateItem } from "../../api/hooks";
import { isActionItem, type FeedbackItem, type Priority, type Retrospective } from "../../api/types";
import { Avatar } from "../../components/ui/Avatar";
import { PriorityBadge } from "../../components/ui/Badge";
import { Button, IconButton } from "../../components/ui/Button";
import { ConfirmDialog } from "../../components/ui/Dialog";
import { TextArea } from "../../components/ui/Field";
import { SegmentedControl } from "../../components/ui/SegmentedControl";
import { useToast } from "../../components/ui/Toaster";
import { cx } from "../../lib/cx";
import { itemVariants, spring } from "../../lib/motion";
import { CompleteControl } from "../actions/CompleteControl";
import { MAX_CONTENT, priorityOptions } from "./Composer";

type Props = { item: FeedbackItem; retro: Retrospective };

/**
 * One item on the board. While the retro is OPEN it can be edited in place and deleted;
 * once CLOSED it is read-only, except that action items can still be marked as done (decision F6).
 */
export function ItemCard({ item, retro }: Props) {
  const [editing, setEditing] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const open = retro.status === "OPEN";
  const action = isActionItem(item) ? item : null;
  const remove = useDeleteItem(retro);
  const { notify } = useToast();

  return (
    <motion.li
      layout="position"
      variants={itemVariants}
      initial="hidden"
      animate="show"
      exit="exit"
      transition={spring}
      className={cx(
        "group relative flex gap-2 rounded-lg border border-line bg-surface p-3 shadow-sm transition-[border-color] duration-150 hover:border-line-strong",
        action?.completed && "bg-surface-2/60",
      )}
    >
      {action && <CompleteControl item={action} teamId={retro.teamId} retroId={retro.id} />}
      <div className={cx("flex min-w-0 flex-1 flex-col gap-2", action && "pt-1")}>
        {editing ? (
          <EditForm item={item} retro={retro} onDone={() => setEditing(false)} />
        ) : (
          <>
            <p className={cx("text-sm leading-relaxed break-words whitespace-pre-wrap", action?.completed ? "text-fg-subtle line-through decoration-fg-subtle/60" : "text-fg")}>
              {item.content}
            </p>
            <div className="flex items-center gap-2">
              <span className="flex min-w-0 items-center gap-1.5 text-xs text-fg-subtle">
                <Avatar name={item.author} decorative />
                <span className="truncate">{item.author}</span>
              </span>
              {action && <PriorityBadge priority={action.priority} />}
              {action?.completed && <span className="text-xs font-medium text-positive">Done</span>}
              {open && (
                <span className="ml-auto flex gap-0.5 opacity-100 transition-opacity duration-150 [@media(hover:hover)]:opacity-0 [@media(hover:hover)]:group-focus-within:opacity-100 [@media(hover:hover)]:group-hover:opacity-100">
                  <IconButton label="Edit" onClick={() => setEditing(true)} className="h-7 w-7">
                    <PencilSimple size={16} />
                  </IconButton>
                  <IconButton label="Delete" tone="danger" onClick={() => setDeleting(true)} className="h-7 w-7">
                    <Trash size={16} />
                  </IconButton>
                </span>
              )}
            </div>
          </>
        )}
      </div>
      <ConfirmDialog
        open={deleting}
        onClose={() => setDeleting(false)}
        title="Delete this item?"
        description={
          <>
            <span className="font-medium text-fg">{item.content.length > 120 ? `${item.content.slice(0, 120)}...` : item.content}</span> will be removed
            from the retrospective. This can't be undone.
          </>
        }
        confirmLabel="Delete"
        onConfirm={async () => {
          await remove.mutateAsync(item);
          notify("Item deleted");
        }}
      />
    </motion.li>
  );
}

function EditForm({ item, retro, onDone }: { item: FeedbackItem; retro: Retrospective; onDone: () => void }) {
  const [content, setContent] = useState(item.content);
  const [priority, setPriority] = useState<Priority>(item.priority ?? "MEDIUM");
  const [error, setError] = useState<string>();
  const update = useUpdateItem(retro);

  async function save() {
    if (content.trim().length === 0) {
      setError("An item can't be empty. Delete it instead.");
      return;
    }
    try {
      await update.mutateAsync({ item, content: content.trim(), priority });
      onDone();
    } catch (failure) {
      setError(messageOf(failure));
    }
  }

  function onKeyDown(event: KeyboardEvent) {
    if (event.key === "Escape") {
      event.preventDefault();
      onDone();
    } else if (event.key === "Enter" && (event.metaKey || event.ctrlKey)) {
      event.preventDefault();
      void save();
    }
  }

  return (
    <div className="flex flex-col gap-3" onKeyDown={onKeyDown}>
      <TextArea label="Edit item" hideLabel value={content} maxLength={MAX_CONTENT} onChange={(event) => setContent(event.target.value)} error={error} autoFocus rows={2} />
      {item.type === "ACTION_ITEM" && <SegmentedControl<Priority> label="Priority" value={priority} onChange={setPriority} options={priorityOptions} />}
      <div className="flex justify-end gap-2">
        <Button size="sm" variant="ghost" onClick={onDone}>
          Cancel
        </Button>
        <Button size="sm" variant="primary" loading={update.isPending} onClick={save}>
          Save
        </Button>
      </div>
    </div>
  );
}

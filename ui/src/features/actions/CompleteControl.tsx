import { CheckCircle, Circle } from "@phosphor-icons/react";
import { useState } from "react";
import { useCompleteActionItem } from "../../api/hooks";
import type { ActionItem } from "../../api/types";
import { ConfirmDialog } from "../../components/ui/Dialog";
import { useToast } from "../../components/ui/Toaster";

type Props = { item: ActionItem; teamId: number; retroId?: number };

/**
 * Completing an action item is one-way (decision A1-A2): the control asks once and says why,
 * and a done item shows a check instead of a checkbox you could untick.
 */
export function CompleteControl({ item, teamId, retroId }: Props) {
  const [confirming, setConfirming] = useState(false);
  const complete = useCompleteActionItem(teamId, retroId);
  const { notify } = useToast();

  if (item.completed) {
    return (
      <span className="inline-flex h-8 w-8 shrink-0 items-center justify-center text-positive" title="Done. Done action items stay done.">
        <CheckCircle weight="fill" size={20} aria-hidden />
        <span className="sr-only">Done</span>
      </span>
    );
  }

  return (
    <>
      <button
        type="button"
        onClick={() => setConfirming(true)}
        aria-label={`Mark as done: ${item.content}`}
        title="Mark as done"
        className="focus-ring group inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-fg-subtle transition-[color,transform] duration-150 hover:text-positive active:scale-[0.92]"
      >
        <Circle size={20} aria-hidden className="group-hover:hidden" />
        <CheckCircle size={20} aria-hidden className="hidden group-hover:block" />
      </button>
      <ConfirmDialog
        open={confirming}
        onClose={() => setConfirming(false)}
        tone="accent"
        title="Mark this action item as done?"
        description={
          <>
            <span className="font-medium text-fg">{item.content}</span> will be marked as done. Done action items stay
            done: RetroFlow doesn't allow reopening them.
          </>
        }
        confirmLabel="Mark as done"
        onConfirm={async () => {
          await complete.mutateAsync(item);
          notify("Action item marked as done");
        }}
      />
    </>
  );
}

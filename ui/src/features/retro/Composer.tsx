import { PaperPlaneRight } from "@phosphor-icons/react";
import { useRef, useState, type FormEvent, type KeyboardEvent } from "react";
import { messageOf } from "../../api/errors";
import { useAddActionItem, useAddFeedback } from "../../api/hooks";
import type { Priority, Retrospective } from "../../api/types";
import { Button } from "../../components/ui/Button";
import { TextArea } from "../../components/ui/Field";
import { SegmentedControl } from "../../components/ui/SegmentedControl";
import type { Column } from "./columns";

export const MAX_CONTENT = 2000;

export const priorityOptions: { value: Priority; label: string }[] = [
  { value: "HIGH", label: "High" },
  { value: "MEDIUM", label: "Medium" },
  { value: "LOW", label: "Low" },
];

type Props = { retro: Retrospective; column: Column; author: string };

/** Adds an item to one column. Ctrl/Cmd + Enter submits; the field keeps focus for the next one. */
export function Composer({ retro, column, author }: Props) {
  const [content, setContent] = useState("");
  const [priority, setPriority] = useState<Priority>("MEDIUM");
  const [error, setError] = useState<string>();
  const input = useRef<HTMLTextAreaElement>(null);
  const addFeedback = useAddFeedback(retro);
  const addAction = useAddActionItem(retro);
  const pending = addFeedback.isPending || addAction.isPending;
  const isAction = column.type === "ACTION_ITEM";

  async function submit(event?: FormEvent) {
    event?.preventDefault();
    const text = content.trim();
    if (text.length === 0) {
      setError("Write something first.");
      return;
    }
    setError(undefined);
    try {
      if (isAction) await addAction.mutateAsync({ content: text, priority, author });
      else await addFeedback.mutateAsync({ content: text, type: column.type as "WENT_WELL" | "NEEDS_IMPROVEMENT", author });
      setContent("");
      input.current?.focus();
    } catch (failure) {
      setError(messageOf(failure));
    }
  }

  function onKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === "Enter" && (event.metaKey || event.ctrlKey)) {
      event.preventDefault();
      void submit();
    }
  }

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-3 rounded-lg border border-line bg-surface p-3 shadow-sm">
      <TextArea
        ref={input}
        label={`Add to ${column.title}`}
        hideLabel
        placeholder={column.prompt}
        value={content}
        maxLength={MAX_CONTENT}
        onChange={(event) => {
          setContent(event.target.value);
          if (error) setError(undefined);
        }}
        onKeyDown={onKeyDown}
        error={error}
        rows={2}
        className="min-h-[4.5rem]"
      />
      <div className="flex flex-wrap items-center justify-between gap-2">
        {isAction ? (
          <SegmentedControl<Priority> label="Priority" value={priority} onChange={setPriority} options={priorityOptions} />
        ) : (
          <span className="hidden text-xs text-fg-subtle sm:inline">Ctrl or Cmd + Enter to add</span>
        )}
        <Button type="submit" variant="primary" size="sm" loading={pending} disabled={!author} icon={<PaperPlaneRight size={16} />} className="ml-auto">
          Add
        </Button>
      </div>
    </form>
  );
}

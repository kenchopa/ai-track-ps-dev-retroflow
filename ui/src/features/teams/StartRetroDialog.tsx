import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { messageOf } from "../../api/errors";
import { useCreateRetrospective } from "../../api/hooks";
import type { Team } from "../../api/types";
import { Button } from "../../components/ui/Button";
import { Dialog } from "../../components/ui/Dialog";
import { TextField } from "../../components/ui/Field";
import { InlineAlert } from "../../components/ui/Feedback";
import { useToast } from "../../components/ui/Toaster";
import { formatDate, todayIso } from "../../lib/dates";

const MAX_TITLE = 200;

export function StartRetroDialog({ team, open, onClose }: { team: Team; open: boolean; onClose: () => void }) {
  const [title, setTitle] = useState("");
  const [date, setDate] = useState("");
  const [titleError, setTitleError] = useState<string>();
  const create = useCreateRetrospective(team.id);
  const navigate = useNavigate();
  const { notify } = useToast();

  useEffect(() => {
    if (!open) return;
    setTitle("");
    setDate("");
    setTitleError(undefined);
    create.reset();
    // Reset only when the dialog opens.
  }, [open]);

  function submit() {
    if (title.trim().length === 0) {
      setTitleError("Give the retrospective a title, for example the sprint it looks back on.");
      return;
    }
    create.mutate(
      { title: title.trim(), date: date || undefined },
      {
        onSuccess: (retro) => {
          notify("Retrospective started");
          onClose();
          navigate(`/retrospectives/${retro.id}`);
        },
      },
    );
  }

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title="Start a retrospective"
      description={`${team.name} can have one open retrospective at a time. Everyone adds feedback until you close it.`}
      onSubmit={submit}
      footer={
        <>
          <Button onClick={onClose}>Cancel</Button>
          <Button type="submit" variant="primary" loading={create.isPending}>
            Start retrospective
          </Button>
        </>
      }
    >
      <TextField
        label="Title"
        value={title}
        maxLength={MAX_TITLE}
        placeholder="Sprint 42"
        autoComplete="off"
        onChange={(event) => {
          setTitle(event.target.value);
          setTitleError(undefined);
        }}
        error={titleError}
        autoFocus
      />
      <TextField
        label="Date"
        type="date"
        optional
        value={date}
        onChange={(event) => setDate(event.target.value)}
        helper={`Leave empty to use today, ${formatDate(todayIso())}.`}
        className="font-mono tabular"
      />
      {create.isError && <InlineAlert tone="danger">{messageOf(create.error)}</InlineAlert>}
    </Dialog>
  );
}

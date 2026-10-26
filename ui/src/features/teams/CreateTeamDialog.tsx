import { X } from "@phosphor-icons/react";
import { useEffect, useId, useState, type KeyboardEvent } from "react";
import { useNavigate } from "react-router";
import { ApiError, messageOf } from "../../api/errors";
import { useCreateTeam } from "../../api/hooks";
import { Avatar } from "../../components/ui/Avatar";
import { Button } from "../../components/ui/Button";
import { Dialog } from "../../components/ui/Dialog";
import { FieldShell, TextField } from "../../components/ui/Field";
import { InlineAlert } from "../../components/ui/Feedback";
import { useToast } from "../../components/ui/Toaster";

const MAX_NAME = 100;

/** The same rules the API applies (decisions T1-T3), checked before sending. */
export function validateTeam(name: string, members: string[]): { name?: string; members?: string } {
  const errors: { name?: string; members?: string } = {};
  if (name.trim().length === 0) errors.name = "Give the team a name.";
  else if (name.trim().length > MAX_NAME) errors.name = `Keep the name under ${MAX_NAME} characters.`;
  if (members.length === 0) errors.members = "Add at least one member.";
  return errors;
}

export function CreateTeamDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const [name, setName] = useState("");
  const [members, setMembers] = useState<string[]>([]);
  const [draft, setDraft] = useState("");
  const [draftError, setDraftError] = useState<string>();
  const [errors, setErrors] = useState<{ name?: string; members?: string }>({});
  const create = useCreateTeam();
  const navigate = useNavigate();
  const { notify } = useToast();
  const membersId = useId();

  useEffect(() => {
    if (!open) return;
    setName("");
    setMembers([]);
    setDraft("");
    setDraftError(undefined);
    setErrors({});
    create.reset();
    // Reset only when the dialog opens, not on every render of the mutation state.
  }, [open]);

  /** Adds the typed name; returns the new member list (or the old one when the name is invalid). */
  function addDraft(): string[] {
    const member = draft.trim().replace(/,$/, "").trim();
    if (member.length === 0) return members;
    if (member.length > MAX_NAME) {
      setDraftError(`Keep names under ${MAX_NAME} characters.`);
      return members;
    }
    if (members.some((existing) => existing.toLowerCase() === member.toLowerCase())) {
      setDraftError(`${member} is already on the team.`);
      return members;
    }
    const next = [...members, member];
    setMembers(next);
    setDraft("");
    setDraftError(undefined);
    setErrors((current) => ({ ...current, members: undefined }));
    return next;
  }

  function onMemberKey(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === "Enter" || event.key === ",") {
      event.preventDefault();
      addDraft();
    } else if (event.key === "Backspace" && draft.length === 0 && members.length > 0) {
      setMembers(members.slice(0, -1));
    }
  }

  function submit() {
    const allMembers = addDraft();
    const found = validateTeam(name, allMembers);
    setErrors(found);
    if (found.name || found.members) return;
    create.mutate(
      { name: name.trim(), members: allMembers },
      {
        onSuccess: (team) => {
          notify(`Team ${team.name} created`);
          onClose();
          navigate(`/teams/${team.id}`);
        },
      },
    );
  }

  const serverError = create.error;
  const nameTaken = serverError instanceof ApiError && serverError.status === 409;

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title="New team"
      description="A team holds retrospectives together. Everyone who adds feedback needs to be a member."
      onSubmit={submit}
      footer={
        <>
          <Button onClick={onClose}>Cancel</Button>
          <Button type="submit" variant="primary" loading={create.isPending}>
            Create team
          </Button>
        </>
      }
    >
      <TextField
        label="Team name"
        value={name}
        maxLength={MAX_NAME}
        autoComplete="off"
        placeholder="Payments squad"
        onChange={(event) => setName(event.target.value)}
        error={errors.name ?? (nameTaken ? serverError.message : undefined)}
        autoFocus
      />
      <FieldShell id={membersId} label="Members" error={errors.members ?? draftError} helper="Type a name and press Enter. Names are matched without regard to case.">
        <div className="flex flex-col gap-2">
          {members.length > 0 && (
            <ul aria-label="Members added" className="flex flex-wrap gap-1.5">
              {members.map((member) => (
                <li key={member} className="flex h-8 items-center gap-1.5 rounded-full border border-line bg-surface-2 pr-1 pl-1 text-sm">
                  <Avatar name={member} decorative />
                  <span className="max-w-[12rem] truncate">{member}</span>
                  <button
                    type="button"
                    aria-label={`Remove ${member}`}
                    onClick={() => setMembers(members.filter((existing) => existing !== member))}
                    className="focus-ring inline-flex h-6 w-6 items-center justify-center rounded-full text-fg-subtle hover:bg-surface hover:text-fg"
                  >
                    <X size={14} />
                  </button>
                </li>
              ))}
            </ul>
          )}
          <div className="flex gap-2">
            <input
              id={membersId}
              value={draft}
              autoComplete="off"
              placeholder={members.length === 0 ? "Mira Okafor" : "Add another member"}
              onChange={(event) => {
                setDraft(event.target.value);
                setDraftError(undefined);
              }}
              onKeyDown={onMemberKey}
              aria-invalid={errors.members || draftError ? true : undefined}
              aria-describedby={errors.members || draftError ? `${membersId}-error` : `${membersId}-helper`}
              className="focus-ring h-10 w-full min-w-0 rounded-md border border-control bg-surface px-3 text-sm text-fg placeholder:text-fg-subtle hover:border-fg-muted aria-[invalid=true]:border-danger"
            />
            <Button onClick={addDraft} disabled={draft.trim().length === 0}>
              Add
            </Button>
          </div>
        </div>
      </FieldShell>
      {serverError && !nameTaken && <InlineAlert tone="danger">{messageOf(serverError)}</InlineAlert>}
    </Dialog>
  );
}

import { CheckCircle, LockSimple } from "@phosphor-icons/react";
import { useState } from "react";
import { useParams } from "react-router";
import { isNotFound, messageOf } from "../../api/errors";
import { useCloseRetrospective, useFeedbackItems, useRetrospective, useTeam } from "../../api/hooks";
import type { Retrospective, Team } from "../../api/types";
import { NotFound } from "../../components/NotFound";
import { Page, PageHeader } from "../../components/Page";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { ConfirmDialog } from "../../components/ui/Dialog";
import { SelectField } from "../../components/ui/Field";
import { InlineAlert, LoadingRegion, Skeleton } from "../../components/ui/Feedback";
import { StatusDot } from "../../components/ui/StatusDot";
import { useToast } from "../../components/ui/Toaster";
import { formatDate } from "../../lib/dates";
import { usePostingAs } from "../../lib/postingAs";
import { SummaryPanel } from "../summary/SummaryPanel";
import { RetroBoard } from "./RetroBoard";

export function RetroPage() {
  const retroId = Number(useParams().retroId);
  const retro = useRetrospective(retroId);
  const team = useTeam(retro.data?.teamId ?? 0, retro.data !== undefined);

  if (retro.isError && isNotFound(retro.error)) return <NotFound message={`Retrospective ${retroId} doesn't exist.`} />;

  if (retro.isError) {
    return (
      <Page>
        <InlineAlert tone="danger" title="This retrospective couldn't be loaded" action={<Button size="sm" onClick={() => retro.refetch()}>Try again</Button>}>
          {messageOf(retro.error)}
        </InlineAlert>
      </Page>
    );
  }

  if (retro.isPending || team.isPending) {
    return (
      <Page>
        <LoadingRegion label="Loading retrospective" className="flex flex-col gap-6">
          <Skeleton className="h-4 w-40" />
          <Skeleton className="h-8 w-72" />
          <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
            <Skeleton className="h-48 rounded-lg" />
            <Skeleton className="h-48 rounded-lg" />
            <Skeleton className="h-48 rounded-lg" />
          </div>
        </LoadingRegion>
      </Page>
    );
  }

  if (team.isError) {
    return (
      <Page>
        <InlineAlert tone="danger" title="The team of this retrospective couldn't be loaded">
          {messageOf(team.error)}
        </InlineAlert>
      </Page>
    );
  }

  return <RetroView retro={retro.data} team={team.data} />;
}

function RetroView({ retro, team }: { retro: Retrospective; team: Team }) {
  const [closing, setClosing] = useState(false);
  const [author, setAuthor] = usePostingAs(team.id, team.members);
  const close = useCloseRetrospective(retro);
  const items = useFeedbackItems(retro.id, retro.status === "OPEN");
  const { notify } = useToast();
  const open = retro.status === "OPEN";

  return (
    <Page>
      <PageHeader
        crumbs={[
          { label: "Teams", to: "/" },
          { label: team.name, to: `/teams/${team.id}` },
          { label: retro.title },
        ]}
        title={retro.title}
        meta={
          <>
            <span className="font-mono text-xs tabular">{formatDate(retro.date)}</span>
            {open ? (
              <Badge tone="accent">
                <StatusDot open />
                Open
              </Badge>
            ) : (
              <Badge>
                <LockSimple size={12} aria-hidden />
                Closed
              </Badge>
            )}
          </>
        }
        actions={
          open && (
            <Button icon={<CheckCircle />} onClick={() => setClosing(true)}>
              Close retrospective
            </Button>
          )
        }
      />

      {open ? (
        <div className="flex flex-col gap-1.5 sm:max-w-xs">
          <SelectField label="Posting as" value={author} onChange={(event) => setAuthor(event.target.value)} helper="RetroFlow has no login. Pick yourself; this browser remembers it.">
            {team.members.map((member) => (
              <option key={member} value={member}>
                {member}
              </option>
            ))}
          </SelectField>
        </div>
      ) : (
        <>
          <InlineAlert tone="info" title="This retrospective is closed">
            Feedback is frozen: nothing can be added, changed or deleted. Action items can still be marked as done.
          </InlineAlert>
          <SummaryPanel retro={retro} itemCount={items.data?.length ?? 0} />
        </>
      )}

      <RetroBoard retro={retro} author={author} />

      <ConfirmDialog
        open={closing}
        onClose={() => setClosing(false)}
        tone="accent"
        title="Close this retrospective?"
        description={
          <ul className="mt-1 flex list-disc flex-col gap-1 pl-5">
            <li>Nobody can add, change or delete feedback anymore.</li>
            <li>Action items can still be marked as done.</li>
            <li>A closed retrospective can't be reopened.</li>
          </ul>
        }
        confirmLabel="Close retrospective"
        onConfirm={async () => {
          await close.mutateAsync();
          notify("Retrospective closed");
        }}
      />
    </Page>
  );
}

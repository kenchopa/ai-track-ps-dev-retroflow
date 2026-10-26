import { ArrowRight, CalendarBlank, Plus } from "@phosphor-icons/react";
import { motion } from "framer-motion";
import { useState } from "react";
import { Link, useParams } from "react-router";
import { isNotFound, messageOf } from "../../api/errors";
import { useRetrospectives, useTeam } from "../../api/hooks";
import type { Retrospective, Team } from "../../api/types";
import { NotFound } from "../../components/NotFound";
import { Page, PageHeader } from "../../components/Page";
import { AvatarStack } from "../../components/ui/Avatar";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { EmptyState, InlineAlert, LoadingRegion, Skeleton } from "../../components/ui/Feedback";
import { StatusDot } from "../../components/ui/StatusDot";
import { TabLinks } from "../../components/ui/Tabs";
import { formatDate } from "../../lib/dates";
import { itemVariants, listVariants } from "../../lib/motion";
import { ActionItemsPanel } from "../actions/ActionItemsPanel";
import { StartRetroDialog } from "./StartRetroDialog";

export function TeamPage({ tab }: { tab: "retrospectives" | "action-items" }) {
  const teamId = Number(useParams().teamId);
  const team = useTeam(teamId);
  const retros = useRetrospectives(teamId);
  const [starting, setStarting] = useState(false);

  if (team.isError && isNotFound(team.error)) return <NotFound message={`Team ${teamId} doesn't exist.`} />;

  if (team.isPending) {
    return (
      <Page>
        <LoadingRegion label="Loading team" className="flex flex-col gap-6">
          <Skeleton className="h-4 w-24" />
          <Skeleton className="h-8 w-64" />
          <Skeleton className="h-10 w-full max-w-md" />
          <Skeleton className="h-40 w-full" />
        </LoadingRegion>
      </Page>
    );
  }

  if (team.isError) {
    return (
      <Page>
        <InlineAlert tone="danger" title="This team couldn't be loaded" action={<Button size="sm" onClick={() => team.refetch()}>Try again</Button>}>
          {messageOf(team.error)}
        </InlineAlert>
      </Page>
    );
  }

  const openRetro = retros.data?.find((retro) => retro.status === "OPEN");

  return (
    <Page>
      <PageHeader
        crumbs={[{ label: "Teams", to: "/" }, { label: team.data.name }]}
        title={team.data.name}
        meta={
          <>
            <AvatarStack names={team.data.members} />
            <span className="min-w-0 truncate">{team.data.members.join(", ")}</span>
          </>
        }
        actions={
          openRetro ? (
            <Link
              to={`/retrospectives/${openRetro.id}`}
              className="focus-ring inline-flex h-10 items-center gap-2 rounded-md bg-accent px-4 text-sm font-medium text-on-accent shadow-sm transition-[background-color,transform] duration-150 hover:bg-accent-strong active:scale-[0.98]"
            >
              Continue {openRetro.title}
              <ArrowRight aria-hidden />
            </Link>
          ) : (
            <Button variant="primary" icon={<Plus />} onClick={() => setStarting(true)} disabled={retros.isPending}>
              Start retrospective
            </Button>
          )
        }
      />

      <TabLinks
        label="Team sections"
        links={[
          { to: `/teams/${teamId}`, label: "Retrospectives", end: true },
          { to: `/teams/${teamId}/action-items`, label: "Action items" },
        ]}
      />

      {tab === "retrospectives" ? (
        <div className="grid grid-cols-1 gap-8 lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
          <RetroTimeline team={team.data} retros={retros} onStart={() => setStarting(true)} />
          <aside className="lg:border-l lg:border-line lg:pl-8">
            <ActionItemsPanel team={team.data} compact />
          </aside>
        </div>
      ) : (
        <ActionItemsPanel team={team.data} />
      )}

      <StartRetroDialog team={team.data} open={starting} onClose={() => setStarting(false)} />
    </Page>
  );
}

type RetrosQuery = ReturnType<typeof useRetrospectives>;

function RetroTimeline({ team, retros, onStart }: { team: Team; retros: RetrosQuery; onStart: () => void }) {
  if (retros.isPending) {
    return (
      <LoadingRegion label="Loading retrospectives" className="flex flex-col gap-2">
        <Skeleton className="h-20 rounded-lg" />
        <Skeleton className="h-14 rounded-lg" />
        <Skeleton className="h-14 rounded-lg" />
      </LoadingRegion>
    );
  }
  if (retros.isError) {
    return (
      <InlineAlert tone="danger" title="Retrospectives couldn't be loaded" action={<Button size="sm" onClick={() => retros.refetch()}>Try again</Button>}>
        {messageOf(retros.error)}
      </InlineAlert>
    );
  }
  if (retros.data.length === 0) {
    return (
      <EmptyState
        icon={<CalendarBlank />}
        title="No retrospectives yet"
        description={`Start the first retrospective for ${team.name}. Everyone adds what went well, what needs to improve and the actions you agree on.`}
        action={
          <Button variant="primary" icon={<Plus />} onClick={onStart}>
            Start retrospective
          </Button>
        }
      />
    );
  }

  const open = retros.data.find((retro) => retro.status === "OPEN");
  const closed = retros.data.filter((retro) => retro.status === "CLOSED");

  return (
    <section aria-labelledby="retros-heading" className="flex flex-col gap-4">
      <h2 id="retros-heading" className="text-base font-semibold tracking-tight text-fg">
        Retrospectives <span className="ml-1 font-mono text-sm font-normal text-fg-subtle tabular">{retros.data.length}</span>
      </h2>
      {open && <OpenRetroCard retro={open} />}
      {closed.length > 0 && (
        <motion.ul variants={listVariants} initial="hidden" animate="show" className="flex flex-col divide-y divide-line overflow-hidden rounded-lg border border-line bg-surface shadow-sm">
          {closed.map((retro) => (
            <motion.li key={retro.id} variants={itemVariants}>
              <Link to={`/retrospectives/${retro.id}`} className="focus-ring group flex items-center gap-3 px-4 py-3.5 transition-colors duration-150 hover:bg-surface-2">
                <span className="min-w-0 flex-1 truncate text-sm font-medium text-fg">{retro.title}</span>
                <span className="font-mono text-xs text-fg-subtle tabular">{formatDate(retro.date)}</span>
                <Badge>Closed</Badge>
                <ArrowRight aria-hidden size={16} className="shrink-0 text-fg-subtle transition-transform duration-150 group-hover:translate-x-0.5 group-hover:text-fg" />
              </Link>
            </motion.li>
          ))}
        </motion.ul>
      )}
    </section>
  );
}

function OpenRetroCard({ retro }: { retro: Retrospective }) {
  return (
    <Link
      to={`/retrospectives/${retro.id}`}
      className="focus-ring group flex items-center gap-4 rounded-lg border border-accent-line bg-accent-soft px-5 py-4 transition-[transform,box-shadow] duration-150 hover:shadow-md active:scale-[0.995]"
    >
      <div className="flex min-w-0 flex-1 flex-col gap-1">
        <span className="flex items-center gap-2 text-xs font-medium tracking-wide text-accent uppercase">
          <StatusDot open />
          Open now
        </span>
        <span className="truncate text-base font-semibold text-fg">{retro.title}</span>
        <span className="font-mono text-xs text-fg-muted tabular">{formatDate(retro.date)}</span>
      </div>
      <span className="flex items-center gap-1.5 text-sm font-medium text-accent">
        Continue
        <ArrowRight aria-hidden className="transition-transform duration-150 group-hover:translate-x-0.5" />
      </span>
    </Link>
  );
}

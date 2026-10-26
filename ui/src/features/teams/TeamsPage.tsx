import { ArrowRight, Plus, UsersThree } from "@phosphor-icons/react";
import { motion } from "framer-motion";
import { Link } from "react-router";
import { messageOf } from "../../api/errors";
import { useOpenRetros, useTeams } from "../../api/hooks";
import { useOpenCreateTeam } from "../../components/AppShell";
import { Page, PageHeader } from "../../components/Page";
import { AvatarStack } from "../../components/ui/Avatar";
import { Button } from "../../components/ui/Button";
import { EmptyState, InlineAlert, LoadingRegion, Skeleton } from "../../components/ui/Feedback";
import { StatusDot } from "../../components/ui/StatusDot";
import { itemVariants, listVariants } from "../../lib/motion";

export function TeamsPage() {
  const teams = useTeams();
  const openCreateTeam = useOpenCreateTeam();
  const open = useOpenRetros(teams.data?.map((team) => team.id) ?? []);

  return (
    <Page>
      <PageHeader
        title="Teams"
        meta="Pick a team to run its retrospective or follow up on its action items."
        actions={
          <Button variant="primary" icon={<Plus />} onClick={openCreateTeam}>
            New team
          </Button>
        }
      />

      {teams.isPending && (
        <LoadingRegion label="Loading teams" className="flex flex-col gap-2">
          {[0, 1, 2].map((row) => (
            <Skeleton key={row} className="h-[72px] rounded-lg" />
          ))}
        </LoadingRegion>
      )}

      {teams.isError && (
        <InlineAlert tone="danger" title="Teams couldn't be loaded" action={<Button size="sm" onClick={() => teams.refetch()}>Try again</Button>}>
          {messageOf(teams.error)}
        </InlineAlert>
      )}

      {teams.data?.length === 0 && (
        <EmptyState
          icon={<UsersThree />}
          title="No teams yet"
          description="RetroFlow keeps the retrospectives of a team together: what went well, what needs to improve, and the action items you agree on. Start by adding your team and its members."
          action={
            <Button variant="primary" icon={<Plus />} onClick={openCreateTeam}>
              Create your first team
            </Button>
          }
        />
      )}

      {teams.data && teams.data.length > 0 && (
        <motion.ul variants={listVariants} initial="hidden" animate="show" className="flex flex-col divide-y divide-line overflow-hidden rounded-lg border border-line bg-surface shadow-sm">
          {teams.data.map((team) => {
            const openRetro = open.get(team.id);
            return (
              <motion.li key={team.id} variants={itemVariants}>
                <Link
                  to={`/teams/${team.id}`}
                  className="focus-ring group flex items-center gap-4 px-4 py-4 transition-colors duration-150 hover:bg-surface-2 sm:px-5"
                >
                  <div className="flex min-w-0 flex-1 flex-col gap-1.5 sm:flex-row sm:items-center sm:gap-4">
                    <span className="truncate text-base font-medium text-fg">{team.name}</span>
                    <span className="flex items-center gap-2 text-sm text-fg-muted">
                      <AvatarStack names={team.members} />
                      <span className="font-mono text-xs tabular">
                        {team.members.length} {team.members.length === 1 ? "member" : "members"}
                      </span>
                    </span>
                  </div>
                  {openRetro && (
                    <span className="inline-flex shrink-0 items-center gap-2 rounded-full bg-accent-soft px-2.5 py-1 text-xs font-medium text-accent">
                      <StatusDot open />
                      Retro open
                    </span>
                  )}
                  <ArrowRight aria-hidden className="shrink-0 text-fg-subtle transition-transform duration-150 group-hover:translate-x-0.5 group-hover:text-fg" />
                </Link>
              </motion.li>
            );
          })}
        </motion.ul>
      )}
    </Page>
  );
}

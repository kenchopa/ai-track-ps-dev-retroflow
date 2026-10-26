import { List, Plus, X } from "@phosphor-icons/react";
import { createContext, useContext, useEffect, useRef, useState } from "react";
import { Link, NavLink, Outlet, useLocation } from "react-router";
import { useOpenRetros, useTeams } from "../api/hooks";
import { CreateTeamDialog } from "../features/teams/CreateTeamDialog";
import { cx } from "../lib/cx";
import { IconButton } from "./ui/Button";
import { Skeleton } from "./ui/Feedback";
import { StatusDot } from "./ui/StatusDot";

const CreateTeamContext = createContext<() => void>(() => undefined);

/** Opens the shared "New team" dialog from anywhere in the app. */
export function useOpenCreateTeam() {
  return useContext(CreateTeamContext);
}

export function AppShell() {
  const [creating, setCreating] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const location = useLocation();

  useEffect(() => setMenuOpen(false), [location.pathname]);

  return (
    <CreateTeamContext.Provider value={() => setCreating(true)}>
      <a
        href="#main"
        className="focus-ring sr-only z-50 rounded-md bg-surface px-3 py-2 text-sm font-medium focus:not-sr-only focus:fixed focus:top-3 focus:left-3"
      >
        Skip to content
      </a>
      <div className="min-h-[100dvh] lg:grid lg:grid-cols-[17rem_minmax(0,1fr)]">
        <aside className="sticky top-0 hidden h-[100dvh] flex-col border-r border-line bg-surface lg:flex">
          <Sidebar onCreateTeam={() => setCreating(true)} />
        </aside>

        <div className="sticky top-0 z-30 flex h-14 items-center justify-between border-b border-line bg-surface/90 px-4 backdrop-blur lg:hidden">
          <Brand />
          <IconButton label="Open navigation" size="md" onClick={() => setMenuOpen(true)}>
            <List />
          </IconButton>
        </div>

        <main id="main" tabIndex={-1} className="min-w-0 outline-none">
          <Outlet />
        </main>
      </div>

      <MobileNav open={menuOpen} onClose={() => setMenuOpen(false)} onCreateTeam={() => setCreating(true)} />
      <CreateTeamDialog open={creating} onClose={() => setCreating(false)} />
    </CreateTeamContext.Provider>
  );
}

function Brand() {
  return (
    <Link to="/" className="focus-ring flex items-center gap-2 rounded-md text-[15px] font-semibold tracking-tight text-fg">
      <svg aria-hidden viewBox="0 0 32 32" className="h-6 w-6">
        <rect width="32" height="32" rx="8" className="fill-accent" />
        <path d="M9 21.5c0-4.7 3.1-8.5 7-8.5s7 3.8 7 8.5" fill="none" className="stroke-on-accent" strokeWidth="2.5" strokeLinecap="round" />
        <circle cx="16" cy="10" r="2.2" className="fill-on-accent" />
      </svg>
      RetroFlow
    </Link>
  );
}

function Sidebar({ onCreateTeam }: { onCreateTeam: () => void }) {
  return (
    <div className="flex h-full flex-col">
      <div className="flex h-14 items-center px-5">
        <Brand />
      </div>
      <TeamNav onCreateTeam={onCreateTeam} />
      <p className="mt-auto border-t border-line px-5 py-4 text-xs leading-relaxed text-fg-subtle">
        Run the retro, close it, follow up on the action items.
      </p>
    </div>
  );
}

function TeamNav({ onCreateTeam }: { onCreateTeam: () => void }) {
  const teams = useTeams();
  const open = useOpenRetros(teams.data?.map((team) => team.id) ?? []);

  return (
    <nav aria-label="Teams" className="flex min-h-0 flex-1 flex-col gap-1 overflow-y-auto px-3 pb-4">
      <div className="flex items-center justify-between px-2 pt-2 pb-1">
        <NavLink to="/" end className={({ isActive }) => cx("focus-ring rounded-sm text-xs font-medium tracking-wide uppercase", isActive ? "text-fg" : "text-fg-subtle hover:text-fg")}>
          Teams
        </NavLink>
        <IconButton label="New team" onClick={onCreateTeam} className="h-7 w-7">
          <Plus size={16} />
        </IconButton>
      </div>
      {teams.isPending && (
        <div className="flex flex-col gap-2 px-2 pt-1">
          <Skeleton className="h-7" />
          <Skeleton className="h-7 w-4/5" />
          <Skeleton className="h-7 w-3/5" />
        </div>
      )}
      {teams.isError && <p className="px-2 text-sm text-fg-muted">Teams couldn't be loaded.</p>}
      {teams.data?.length === 0 && <p className="px-2 text-sm text-fg-muted">No teams yet.</p>}
      <ul className="flex flex-col gap-0.5">
        {teams.data?.map((team) => {
          const openRetro = open.get(team.id);
          return (
            <li key={team.id}>
              <NavLink
                to={`/teams/${team.id}`}
                className={({ isActive }) =>
                  cx(
                    "focus-ring flex h-9 items-center justify-between gap-2 rounded-md px-2 text-sm transition-colors duration-150",
                    isActive ? "bg-surface-2 font-medium text-fg" : "text-fg-muted hover:bg-surface-2 hover:text-fg",
                  )
                }
              >
                <span className="truncate">{team.name}</span>
                {openRetro && (
                  <span className="flex items-center gap-1.5 text-xs text-accent" title={`Open retrospective: ${openRetro.title}`}>
                    <StatusDot open />
                    <span className="sr-only">has an open retrospective</span>
                  </span>
                )}
              </NavLink>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}

/** Below lg the navigation is a sheet from the left (a native modal dialog). */
function MobileNav({ open, onClose, onCreateTeam }: { open: boolean; onClose: () => void; onCreateTeam: () => void }) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);

  return (
    <dialog
      ref={ref}
      aria-label="Navigation"
      onClose={onClose}
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
      onClick={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
      className="rf-dialog m-0 h-[100dvh] max-h-none w-[min(20rem,85vw)] border-r border-line bg-surface p-0 text-fg shadow-lg lg:hidden"
    >
      <div className="flex h-full flex-col">
        <div className="flex h-14 items-center justify-between px-4">
          <Brand />
          <IconButton label="Close navigation" size="md" onClick={onClose}>
            <X />
          </IconButton>
        </div>
        <TeamNav
          onCreateTeam={() => {
            onClose();
            onCreateTeam();
          }}
        />
      </div>
    </dialog>
  );
}

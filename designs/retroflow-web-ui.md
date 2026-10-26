# Blueprint: RetroFlow Web UI

## Design Brief

A web interface on top of the existing RetroFlow REST API (Spring Boot, `/api/...`). It is for
the people who hold sprint retrospectives: a team lead who starts and closes the retro, and team
members who add what went well, what needs improvement and action items. Today they can only use
`curl`. The UI covers every feature the API offers:

- **Teams:** list, create (name + members), view.
- **Retrospectives:** start (one OPEN per team), list per team, view, close (one-way: a closed retro
  freezes its feedback).
- **Feedback items:** add (WENT_WELL / NEEDS_IMPROVEMENT), edit, delete, while the retro is OPEN.
- **Action items:** add with priority, edit, complete (one-way; still allowed after close), filter
  across all of a team's retros by priority and completion.
- **AI summary** of a CLOSED retro (`POST /api/retrospectives/{id}/summary`), with every failure
  mode the API returns (409, 400, 502, 503, 504).

The core experience goal is **zero-training use**. Someone who has never seen RetroFlow should
see at a glance:
- which retro is running
- what they can do right now
- why something is not allowed: closed means frozen, a completed item stays completed, one open
  retro per team

The business rules from the brief must be visible in the UI, not only as error messages after the
fact.

## Objective

When this blueprint is complete, `ui/` contains a React + TypeScript single-page app with these
properties:
- **Runs everywhere:** `npm run dev` (proxying `/api` to `localhost:8080`) and `docker compose up`
  (nginx on port 5173, proxying to the `app` service).
- **Every API endpoint** is reachable through the interface, with loading, empty and error states.
- **Responsive and accessible:** it works from 320 px to 1440 px and meets WCAG AA.
- **Documented tokens:** the design tokens are in `DESIGN.md` at the repository root.

## Visual Direction

- **Aesthetic**: design-engineering (metric-based UI/UX rules, `tools.cc.skills.design-engineering`), with the dials adapted to the request "very user-friendly and intuitive": **DESIGN_VARIANCE 5, MOTION_INTENSITY 4, VISUAL_DENSITY 5**. The default 8/6/4 is meant for expressive marketing work; a daily team tool needs predictable placement, motion that explains state changes, and normal app density.
- **Mood**: calm, focused and trustworthy. A workbench, not a showroom.
- **Color Direction**: warm-neutral Zinc base. **One** desaturated accent, deep teal (`#0f766e`), for primary actions and focus. Functional category markers:
  - went well: muted emerald
  - needs improvement: muted amber
  - action item: the accent
  - destructive: muted rose

  Markers always pair with an icon and a label, never color alone. No purple, no gradients, no glow, no pure black.
- **Typography Direction**: Geist (sans) for all UI text and Geist Mono for dates, ids, counts and priorities. No serif and no Inter. A restrained hierarchy, with weight and color doing the work instead of size.
- **Reference Inspirations**: Linear (issue lists and keyboard-first editing), Vercel dashboard (Geist, Zinc, quiet borders), GitHub Projects board columns, Height (inline item editing).

## Component Inventory

Primitives (`ui/src/components/ui/`):

- `Button` — `variant: primary | secondary | ghost | danger`, `size: sm | md`, `loading` (inline skeleton bar, label stays for width stability), tactile `:active` `scale-[0.98]`.
- `IconButton` — square ghost button with a required `label` (used as `aria-label` and tooltip).
- `TextField` / `TextArea` — label above, optional helper, error below (`aria-invalid`, `aria-describedby`), `gap-2`; TextArea auto-grows, shows a character count near the 2000-character limit.
- `SelectField` — native `<select>` styled with the field shell (accessible on mobile).
- `SegmentedControl` — radio-group semantics (`role="radiogroup"`), used for filters and type pickers.
- `Badge` — `tone: neutral | positive | attention | accent | danger`; priority and status pills, mono text.
- `Avatar` — initials on a deterministic tinted background derived from the name (no egg icons); `AvatarStack` for members.
- `Skeleton` — shimmer block sized to the content it replaces; static under reduced motion.
- `EmptyState` — icon, one-line title, one-line explanation, and the action that fills it.
- `InlineAlert` — `tone: info | warning | danger`, `role="status"` or `role="alert"`.
- `Dialog` — native `<dialog>` with `showModal()` (built-in focus trap, Escape, `::backdrop`), titled via `aria-labelledby`.
- `ConfirmDialog` — Dialog with explanation and a destructive or one-way confirm.
- `Tabs` — `role="tablist"` with arrow-key navigation; used for the team page and the mobile board.
- `Toaster` — polite `aria-live` region for short confirmations ("Retro closed").
- `StatusDot` — memoized, isolated leaf; "breathing" opacity loop only for OPEN, static otherwise and under reduced motion.

Composed (`ui/src/features/...`):

- `AppShell` — sidebar (lg and up) or top bar + drawer (below lg), content area `max-w-[1400px]`.
- `TeamNav` — team list with an OPEN indicator, a "New team" action, and the active route highlighted.
- `CreateTeamDialog` — name + member chip input (Enter or comma adds a member, Backspace removes the last one); inline duplicate checks mirroring the API rules.
- `TeamsOverview` — the home page: team rows (name, members, open retro); the empty state is a first-run welcome with create-team.
- `TeamHeader` — name, `AvatarStack`, "Start retrospective" or "Continue open retro".
- `StartRetroDialog` — title + optional date (defaults to today, as in decision R1).
- `RetroTimeline` — the open retro pinned on top as a highlighted row, then closed retros newest first (mono dates).
- `ActionItemsPanel` — filters (priority SegmentedControl, status SegmentedControl) plus a list across all retros; each row links to its retro.
- `RetroHeader` — breadcrumb (Team / Retro), title, mono date, status badge with StatusDot, and the "Close retrospective" action.
- `CloseRetroDialog` — explains the consequence in plain words before confirming.
- `ClosedBanner` — "Closed on … Feedback is frozen. Action items can still be completed."
- `PostingAs` — member picker ("Posting as Alice"), remembered per team in `localStorage`.
- `RetroBoard` — three columns (Went well / Needs improvement / Action items) on md and up, Tabs with counts below md.
- `BoardColumn` — header (icon, label, count), composer, animated list, empty state.
- `FeedbackComposer` — textarea + submit (Ctrl/Cmd+Enter); the action-item variant adds a priority picker.
- `FeedbackCard` — content, author avatar, edit (inline, Escape cancels) and delete (confirm); read-only with a lock icon when closed.
- `ActionItemCard` — priority badge, completion checkbox (one-way, explained), edit and delete while open.
- `SummaryPanel` — closed retros only; states: idle (explains what happens and that feedback goes to OpenAI), generating (skeleton shimmer in the final layout), result (three sections + regenerate), and errors mapped per status.

## Page Layouts

### Teams overview (`/`)

- **Purpose**: pick a team, or create the first one.
- **Sections**:
  1. Page header: left-aligned title "Teams" + "New team" button on the right.
  2. Team list: rows with name, AvatarStack, and an "Open retro" badge with StatusDot when one is running. Each row links to the team.
  3. Empty state on first run: short explanation of RetroFlow in one sentence, plus the create-team action.
- **Responsive Notes**: single column at all sizes; the rows stack their meta under the name below sm.

### Team (`/teams/:teamId`, `/teams/:teamId/action-items`)

- **Purpose**: the team's home. Start or continue the retro, see history, follow up on actions.
- **Sections**:
  1. TeamHeader (name, members, primary action).
  2. Tabs: "Retrospectives" | "Action items", one URL per tab.
  3. Retrospectives tab: RetroTimeline (the open retro highlighted on top). Empty state: "No retrospectives yet" + Start retrospective.
  4. Action items tab: filters bar + list. Empty state per filter ("No open high-priority action items").
- **Responsive Notes**:
  - **lg and up:** a 2fr/1fr grid, with the timeline on the left and a compact "Open action items" preview on the right.
  - **Below lg:** a single column; the preview moves under the timeline and becomes the tab.

### Retrospective board (`/retrospectives/:retroId`)

- **Purpose**: where the retro happens.
- **Sections**:
  1. RetroHeader (breadcrumb, title, date, status, Close).
  2. ClosedBanner (only when closed).
  3. PostingAs (only when open).
  4. RetroBoard: three columns, each with a composer on top (hidden when closed) and the item list.
  5. SummaryPanel (only when closed), placed above the board, because after closing the summary is the next thing to do.
- **Responsive Notes**:
  - **md and up:** three equal columns (functional parity of the three kinds; this is not the banned marketing "3-card row").
  - **Below md:** Tabs with counts, one column at a time, the composer sticky at the top of the tab.
  - Horizontal scroll never happens: the content is contained at 320 px.

### Not found / error route

- **Purpose**: unknown team or retro id (API 404).
- **Sections**: message using the API's reason ("Retrospective 42 was not found") + a link back to Teams.
- **Responsive Notes**: single column.

## Motion Spec

- **Animation Strategy**: motion explains a change and never decorates.
  - List items enter with a short stagger.
  - Removed items collapse via `AnimatePresence`.
  - A completed action item re-sorts with `layout`.
  - The summary result fades in, section by section.
  - The only perpetual motion is the OPEN StatusDot "breathing", isolated and memoized.
- **Spring Constants**: `{ type: "spring", stiffness: 100, damping: 20, mass: 1 }` for layout and list moves (from the skill); `{ stiffness: 400, damping: 30 }` for small press feedback.
- **Duration Curves**:
  - 150 ms `cubic-bezier(0.16, 1, 0.3, 1)` for hover and color.
  - 200 ms for dialog fade/scale (0.98 → 1).
  - 40 ms stagger between list items, capped at 8 items.
  - StatusDot breathing: a 2.4 s opacity loop.
- **Interaction States**:
  - **hover:** border/bg tint.
  - **focus-visible:** 2 px accent ring with an offset, on every interactive element.
  - **active:** `scale-[0.98]`.
  - **disabled:** 50% opacity plus a tooltip or helper explaining *why*. Only `transform` and `opacity` are animated.
- **Scroll Animations**: none (MOTION_INTENSITY 4). `MotionConfig reducedMotion="user"` disables movement for `prefers-reduced-motion`.

## Problem Statement

RetroFlow's rules live in the backend: one open retro per team, a closed retro is frozen, a
completed action item can't be undone, and only a closed retro can be summarised. A raw API
surfaces them only as 409 messages after the user has already tried. A team tool used once a
sprint must make these rules *visible before* the action. It must also stay fast for the five
minutes in which a whole team types feedback at once.

## Solution Approach

- **Rules as affordances, not errors:**
  - "Start retrospective" turns into "Continue open retro" when one exists.
  - Composers disappear and a lock plus the ClosedBanner appear when closed.
  - The completion checkbox is one-way, with its consequence in the label.
  - Closing asks for confirmation with a plain-language explanation.
  - Summary only appears for closed retros.
  - Server errors are still shown inline (the API stays the source of truth), with the
    `Entity id:` prefix turned into a readable sentence.
- **Data layer:** TanStack Query, so every view gets loading, error and caching for free.
  Mutations invalidate exactly the affected queries (feedback list, team action items, retro
  list). Optimistic updates are used for completing an action item.
- **Identity without auth:** the API needs an author who is a team member, and there's no login
  (decision G6). A per-team "Posting as" picker, remembered in `localStorage`, makes that explicit.
- **Architecture:** a Vite SPA instead of Next.js. There's no SEO need, every view is
  interactive, and a static build can be served by nginx next to the API. This is the "React"
  branch of the skill's "React or Next.js" default. The structure is feature folders plus shared
  primitives.
- **Theming:** tokens as CSS custom properties in a Tailwind v4 `@theme`, with a
  `prefers-color-scheme: dark` override, documented in `DESIGN.md`.

## Relevant Files

Use these files to complete the task:

- `decisions.md` — the endpoint contract, status codes and the rules the UI must reflect (R1, R3-R5, F1-F7, A1-A3, G3, G6).
- `src/main/java/com/stackcraft/retroflow/controller/*.java` and `dto/*.java` — the exact request and response shapes.
- `src/main/java/com/stackcraft/retroflow/exception/GlobalExceptionHandler.java` — the error body `{error, status, timestamp}` and the status mapping (including the summary's 502/503/504 and `Retry-After`).
- `docker-compose.yml` — gets a `ui` service.
- `TEAM-KIT.md`, `README.md` — get a short "UI" section.
- `/private/tmp/.../design-skill/SKILL.md` (design-engineering) — the rules this UI must pass.

### New Files

- `DESIGN.md` — tokens, type scale, spacing, radii, shadows, motion constants, the dials used.
- `designs/retroflow-web-ui.md` — this blueprint.
- `ui/package.json`, `ui/package-lock.json`, `ui/tsconfig.json`, `ui/vite.config.ts`, `ui/index.html`
- `ui/src/main.tsx`, `ui/src/App.tsx` (router), `ui/src/styles.css` (Tailwind + tokens)
- `ui/src/api/client.ts`, `ui/src/api/types.ts`, `ui/src/api/hooks.ts`, `ui/src/api/errors.ts`
- `ui/src/components/ui/*.tsx` (primitives), `ui/src/components/AppShell.tsx`
- `ui/src/features/teams/*.tsx`, `ui/src/features/retro/*.tsx`, `ui/src/features/actions/*.tsx`, `ui/src/features/summary/*.tsx`
- `ui/src/lib/*.ts` (avatar tint, posting-as storage, dates)
- `ui/src/**/*.test.tsx` (Vitest + Testing Library)
- `ui/Dockerfile`, `ui/nginx.conf`, `ui/.gitignore`, `ui/README.md`

## Implementation Phases

### Phase 1: Foundation

Scaffold `ui/` with Vite + React + TS. Set up Tailwind v4 (`@tailwindcss/vite`), with the
tokens as CSS variables and the dark override. Load Geist and Geist Mono via `@fontsource-variable`.
Then the typed API client with `ApiError`, the TanStack Query provider, the router, and `DESIGN.md`.

### Phase 2: Core Implementation

Build the primitives, then AppShell + TeamNav, the Teams overview + CreateTeamDialog, the Team
page (timeline, start retro, action-items panel with filters), and the Retro board (header, close,
posting-as, columns, composers, cards, edit/delete/complete). Then the SummaryPanel with every
state, and the motion layer (stagger, presence, layout, StatusDot).

### Phase 3: Integration & Polish

Add the Docker image (nginx + `/api` proxy) and the compose service, plus tests for the API
error mapping, the rule-driven UI (closed, open, one-way) and the summary states. Then the
responsive pass at 320/768/1024/1440, an accessibility pass (labels, focus order, contrast,
keyboard paths), screenshots via headless Chrome against the running stack, and the docs.

## Team Orchestration

- You operate as the team lead and orchestrate the team to execute the blueprint.
- You're responsible for deploying the right team members with the right context to execute the blueprint.
- IMPORTANT: You NEVER operate directly on the design artifacts. You use `Task` and `Task*` tools to deploy team members to do the building, validating, testing, and integration tasks.
  - This is critical. Your job is to act as a high level director of the design team, not a builder.
  - Your role is to validate all work is going well and make sure the team is on track to complete the blueprint.
  - You'll orchestrate this by using the Task\* Tools to manage coordination between the team members.
  - Communication is paramount. You'll use the Task\* Tools to communicate with the team members and ensure they're on track to complete the blueprint.
- Take note of the session id of each team member. This is how you'll reference them.

### Team Members

- Builder
  - Name: tokens-architect
  - Role: design token author (DESIGN.md, Tailwind theme, fonts, base styles)
  - Agent Type: design-system-architect
  - Resume: true
- Builder
  - Name: ui-builder-primitives
  - Role: component architect for primitives and the API/data layer
  - Agent Type: ui-builder
  - Resume: true
- Builder
  - Name: layout-builder
  - Role: page layout builder (AppShell, Teams, Team, Retro board, responsive behaviour)
  - Agent Type: layout-specialist
  - Resume: true
- Builder
  - Name: motion-engineer
  - Role: animation engineer (stagger, presence, layout, StatusDot, reduced motion)
  - Agent Type: motion-specialist
  - Resume: true
- Builder
  - Name: design-validator
  - Role: read-only reviewer (skill rules, WCAG AA, consistency with DESIGN.md, screenshots)
  - Agent Type: design-reviewer
  - Resume: false

## Step by Step Tasks

- IMPORTANT: Execute every step in order, top to bottom. Each task maps directly to a `TaskCreate` call.
- Before you start, run `TaskCreate` to create the initial task list that all team members can see and execute.

### 1. Define design tokens and scaffold

- **Task ID**: define-design-tokens
- **Depends On**: none
- **Assigned To**: tokens-architect
- **Agent Type**: design-system-architect
- **Parallel**: false
- Scaffold `ui/` (Vite + React + TS) with the pinned dependencies from Notes. Install with `npm install --save-exact`.
- Write `ui/src/styles.css`:
  - Tailwind v4 `@import "tailwindcss"` and `@theme` tokens (colors, fonts, radii, shadows).
  - A `prefers-color-scheme: dark` override.
  - A focus-ring utility.
- Write `DESIGN.md` with every token, the type scale, spacing, motion constants and the dials (5/4/5) with the reason.

### 2. API layer and primitives

- **Task ID**: build-primitives
- **Depends On**: define-design-tokens
- **Assigned To**: ui-builder-primitives
- **Agent Type**: ui-builder
- **Parallel**: false
- `api/types.ts` mirrors the DTOs.
- `api/client.ts`: `fetch` wrapper that throws `ApiError {status, message, reason, retryAfter}`.
- `api/errors.ts`: `reasonOf("Retrospective 4: already closed")` returns "Already closed".
- `api/hooks.ts`: queries and mutations with targeted invalidation.
- Every primitive from the inventory, with full states and ARIA. Native `<dialog>` for Dialog.

### 3. Shell and pages

- **Task ID**: compose-pages
- **Depends On**: build-primitives
- **Assigned To**: layout-builder
- **Agent Type**: layout-specialist
- **Parallel**: false
- AppShell + TeamNav + router (`/`, `/teams/:teamId`, `/teams/:teamId/action-items`, `/retrospectives/:retroId`, fallback).
- Teams overview, CreateTeamDialog, Team page (timeline, StartRetroDialog, ActionItemsPanel with filters).
- Retro board: header, CloseRetroDialog, ClosedBanner, PostingAs, columns, composers, cards; mobile Tabs.
- SummaryPanel with the states idle / generating / result / error (409, 400, 502, 503 + Retry-After, 504).

### 4. Motion layer

- **Task ID**: add-motion
- **Depends On**: compose-pages
- **Assigned To**: motion-engineer
- **Agent Type**: motion-specialist
- **Parallel**: false
- `MotionConfig reducedMotion="user"` at the root.
- List stagger, `AnimatePresence` exits, `layout` re-sort on completion, dialog scale, summary section reveal.
- StatusDot breathing as an isolated `React.memo` leaf.

### 5. Packaging, tests and docs

- **Task ID**: integrate-and-test
- **Depends On**: add-motion
- **Assigned To**: ui-builder-primitives
- **Agent Type**: ui-builder
- **Parallel**: false
- `ui/Dockerfile` (node build stage, then nginx), `ui/nginx.conf` (SPA fallback + `/api` proxy to `app:8080`), compose service `ui` on port 5173.
- Vitest + Testing Library tests:
  - error mapping
  - a closed retro hides composers and shows the banner
  - the one-way completion checkbox
  - the summary error states
  - create-team validation
- README/TEAM-KIT "UI" section.

### 6. Validate everything

- **Task ID**: validate-all
- **Depends On**: define-design-tokens, build-primitives, compose-pages, add-motion, integrate-and-test
- **Assigned To**: design-validator
- **Agent Type**: design-reviewer
- **Parallel**: false
- Run all validation commands.
- Verify components render correctly across breakpoints: screenshots at 390, 768, 1280 and 1440 px of the Teams, Team, open board, closed board and summary views.
- Verify accessibility requirements are met: labels, focus-visible, keyboard paths, contrast of every token pair.
- Verify the acceptance criteria are met, and check the design-engineering pre-flight checklist (section 10).

## Acceptance Criteria

- Every API endpoint in `decisions.md`'s contract plus the summary endpoint is reachable from the UI.
- Every view has a loading skeleton, an empty state that says how to fill it, and inline error reporting.
- The business rules are visible before the action:
  - start vs continue (one open retro)
  - composers hidden and a lock shown when closed
  - one-way completion, explained
  - close confirmation
  - summary only for closed retros
- Every server error is shown in plain language. The summary's 502/503/504/400/409 each get a specific message and a next step (retry, wait for `Retry-After`, ask an admin to set the key).
- WCAG AA: text contrast of at least 4.5:1 (3:1 for large text and UI borders); every control is keyboard reachable with a visible focus ring; dialogs trap focus and close on Escape; every input has a label; status changes are announced.
- Responsive at 320/768/1024/1440 px with no horizontal page scroll; the board becomes tabs below 768 px.
- Every animation respects `prefers-reduced-motion`; only `transform` and `opacity` are animated.
- Skill rules:
  - Geist/Geist Mono (no Inter)
  - one accent, no purple and no gradients
  - no emoji
  - no pure black
  - `min-h-[100dvh]` instead of `h-screen`
  - Phosphor icons with a single weight
- `npm run typecheck`, `npm test` and `npm run build` pass. `mvn verify` still passes, because the backend is unchanged.

## Validation Commands

Execute these commands to validate the blueprint is complete:

- `cd ui && npm ci` - Install the exact pinned dependencies
- `cd ui && npm run typecheck` - Type-check all components (`tsc --noEmit`)
- `cd ui && npm test` - Vitest + Testing Library component and API tests
- `cd ui && npm run build` - Production build succeeds
- `docker compose up --build -d app ui` - Run the full stack; the UI is on http://localhost:5173
- `curl -s localhost:5173/api/teams` - The nginx proxy reaches the API
- Headless Chrome screenshots at 390/768/1280/1440 px of each view - Visual and responsive check
- `grep -rnP "[\x{1F300}-\x{1FAFF}\x{2600}-\x{27BF}]" ui/src` - No emoji anywhere (must be empty)
- `grep -rn "h-screen\|font-inter\|Inter" ui/src` - Banned patterns (must be empty)
- `docker run --rm -v "$PWD":/workspace -w /workspace maven:3.9-eclipse-temurin-21 mvn -B verify` - Backend still green

## Notes

- **Stack and exact versions.** Every version was checked on npm and is at least two weeks old on
  2026-10-26:
  - runtime: `react@19.3.0`, `react-dom@19.3.0`, `react-router@7.18.4`,
    `@tanstack/react-query@5.104.2`, `framer-motion@14.1.0`, `@phosphor-icons/react@2.1.10`,
    `@fontsource-variable/geist@5.3.0`, `@fontsource-variable/geist-mono@5.3.0`
  - dev: `vite@8.3.4`, `@vitejs/plugin-react@6.1.2`, `typescript@7.0.2`, `tailwindcss@4.3.3`,
    `@tailwindcss/vite@4.3.3`, `@types/react@19.3.0`, `@types/react-dom@19.3.0`, `vitest@5.0.4`,
    `@testing-library/react@16.3.3`, `@testing-library/jest-dom@7.0.1`,
    `@testing-library/user-event@14.6.7`, `jsdom@30.1.2`
  - react-router is deliberately the newest 7.x, not 8.x: the 7.x data-router API is the one this
    plan is written against.
  - Install command: `npm install --save-exact react@19.3.0 react-dom@19.3.0 react-router@7.18.4 @tanstack/react-query@5.104.2 framer-motion@14.1.0 @phosphor-icons/react@2.1.10 @fontsource-variable/geist@5.3.0 @fontsource-variable/geist-mono@5.3.0` and `npm install --save-exact -D vite@8.3.4 @vitejs/plugin-react@6.1.2 typescript@7.0.2 tailwindcss@4.3.3 @tailwindcss/vite@4.3.3 @types/react@19.3.0 @types/react-dom@19.3.0 vitest@5.0.4 @testing-library/react@16.3.3 @testing-library/jest-dom@7.0.1 @testing-library/user-event@14.6.7 jsdom@30.1.2`.
- **No backend changes:** the UI uses the existing API. Same origin via the Vite dev proxy and
  nginx, so no CORS configuration is needed.
- **The AI summary sends feedback to OpenAI** (see `bonus-findings.md`). The SummaryPanel says so
  before the first click.
- **Browser support:** current Chrome, Edge, Firefox and Safari (`<dialog>` and `dvh` are needed).
- **Execution note for this session:** the `Agent`/`Task` tool is denied by a permission rule
  here, so the team lead executes the tasks directly, in the order above, and logs every step in
  `prompt-log.md`.

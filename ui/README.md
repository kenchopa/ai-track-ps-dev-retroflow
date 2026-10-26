# RetroFlow web UI

A React + TypeScript single-page app on top of the RetroFlow API. It covers every endpoint: teams,
retrospectives (start, close), feedback and action items (add, edit, delete, mark as done, filter
per team), and the AI summary of a closed retrospective.

- **Design:** `designs/retroflow-web-ui.md` (the blueprint) and `DESIGN.md` (the tokens), both at
  the repository root.
- **Stack:** Vite 8, React 19, TypeScript 7, Tailwind CSS 4, TanStack Query 5, React Router 7,
  framer-motion and Phosphor icons. Every version is pinned exactly in `package.json`.

## Run it

| How | Command | URL |
|---|---|---|
| Everything in Docker | `docker compose up --build app ui` (from the repo root) | http://localhost:5173 |
| UI dev server against a running API | `npm ci && npm run dev` (the API on :8080, e.g. `docker compose up app`) | http://localhost:5173 |

The UI and the API share one origin, through the Vite dev proxy or nginx, so there's no CORS
setup. For the AI summary, the **API** needs `OPENAI_API_KEY` in its environment. The UI never sees
the key.

## Check it

```bash
npm run typecheck   # tsc --noEmit
npm test            # Vitest + Testing Library, with fetch stubbed (no server, no OpenAI)
npm run build       # production build in dist/
```

## How it's organised

| Folder | What's in it |
|---|---|
| `src/api/` | Typed client (`client.ts`), errors in plain language (`errors.ts`), TanStack Query hooks with targeted invalidation (`hooks.ts`) |
| `src/components/ui/` | Primitives: Button, fields, SegmentedControl, Badge, Avatar, Skeleton/EmptyState/InlineAlert, native `<dialog>`, Tabs, Toaster, StatusDot |
| `src/features/` | `teams/` (overview, team page, create and start dialogs), `retro/` (board, composer, item cards), `actions/` (team action items, the one-way "mark as done"), `summary/` (the AI summary panel) |

## Notes

- **`.npmrc` sets `legacy-peer-deps`.** The optional peers of vitest 5.0.x (browser-playwright,
  msw, devtools) crash npm's peer resolver ("reading edgesOut"). The real peers are pinned
  explicitly, including `@testing-library/dom`.
- **The nginx image is unprivileged:** non-root, port 8080 inside the container. It adds a strict
  Content-Security-Policy and the other security headers to every response.

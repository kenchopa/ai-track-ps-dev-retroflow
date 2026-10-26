import { IconContext } from "@phosphor-icons/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MotionConfig } from "framer-motion";
import { createBrowserRouter, RouterProvider } from "react-router";
import { ApiError } from "./api/errors";
import { AppShell } from "./components/AppShell";
import { NotFound, RouteError } from "./components/NotFound";
import { Toaster } from "./components/ui/Toaster";
import { RetroPage } from "./features/retro/RetroPage";
import { TeamPage } from "./features/teams/TeamPage";
import { TeamsPage } from "./features/teams/TeamsPage";

export function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 10_000,
        // A 4xx answer won't change by asking again; network and 5xx errors get two more tries.
        retry: (failures, error) => !(error instanceof ApiError && error.status >= 400 && error.status < 500) && failures < 2,
      },
    },
  });
}

const queryClient = createQueryClient();

export const routes = [
  {
    element: <AppShell />,
    errorElement: <RouteError />,
    children: [
      { index: true, element: <TeamsPage /> },
      { path: "teams/:teamId", element: <TeamPage tab="retrospectives" /> },
      { path: "teams/:teamId/action-items", element: <TeamPage tab="action-items" /> },
      { path: "retrospectives/:retroId", element: <RetroPage /> },
      { path: "*", element: <NotFound /> },
    ],
  },
];

const router = createBrowserRouter(routes);

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <MotionConfig reducedMotion="user">
        <IconContext.Provider value={{ weight: "regular", size: 18 }}>
          <Toaster>
            <RouterProvider router={router} />
          </Toaster>
        </IconContext.Provider>
      </MotionConfig>
    </QueryClientProvider>
  );
}

import { QueryClientProvider } from "@tanstack/react-query";
import { render } from "@testing-library/react";
import { MotionConfig } from "framer-motion";
import { createMemoryRouter, RouterProvider } from "react-router";
import { vi } from "vitest";
import { createQueryClient, routes } from "../App";
import { Toaster } from "../components/ui/Toaster";

type Reply = { status?: number; body?: unknown; headers?: Record<string, string> };
type Handler = Reply | ((body: unknown) => Reply);

/** Stubs fetch with "METHOD /api/path" handlers and records every call. Unknown calls fail the test loudly. */
export function mockApi(handlers: Record<string, Handler>) {
  const calls: { method: string; path: string; body: unknown }[] = [];
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const method = init?.method ?? "GET";
    const path = String(input);
    const body = init?.body ? JSON.parse(String(init.body)) : undefined;
    calls.push({ method, path, body });
    const handler = handlers[`${method} ${path}`];
    if (!handler) throw new Error(`No mock for ${method} ${path}`);
    const reply = typeof handler === "function" ? handler(body) : handler;
    const status = reply.status ?? 200;
    return new Response(status === 204 ? null : JSON.stringify(reply.body ?? null), {
      status,
      headers: { "Content-Type": "application/json", ...reply.headers },
    });
  });
  vi.stubGlobal("fetch", fetchMock);
  return calls;
}

/** The real routes and providers, at a given URL, with a fresh cache and no retries. */
export function renderApp(url: string) {
  const client = createQueryClient();
  client.setDefaultOptions({ queries: { retry: false, staleTime: Infinity }, mutations: { retry: false } });
  const router = createMemoryRouter(routes, { initialEntries: [url] });
  return render(
    <QueryClientProvider client={client}>
      <MotionConfig reducedMotion="always">
        <Toaster>
          <RouterProvider router={router} />
        </Toaster>
      </MotionConfig>
    </QueryClientProvider>,
  );
}

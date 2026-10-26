import { ApiError, NETWORK_ERROR } from "./errors";
import type {
  ActionItem,
  ActionItemFilter,
  ApiErrorBody,
  FeedbackItem,
  Priority,
  Retrospective,
  Summary,
  Team,
} from "./types";

type Method = "GET" | "POST" | "PUT" | "DELETE";

async function request<T>(method: Method, path: string, body?: unknown): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`/api${path}`, {
      method,
      headers: body === undefined ? { Accept: "application/json" } : { Accept: "application/json", "Content-Type": "application/json" },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, NETWORK_ERROR);
  }
  if (response.status === 204) return undefined as T;
  const data = await readJson(response);
  if (!response.ok) {
    const message = (data as ApiErrorBody | undefined)?.error ?? `Request failed with status ${response.status}`;
    const retryAfter = Number(response.headers.get("Retry-After")) || undefined;
    throw new ApiError(response.status, message, retryAfter);
  }
  return data as T;
}

async function readJson(response: Response): Promise<unknown> {
  const text = await response.text();
  if (text.length === 0) return undefined;
  try {
    return JSON.parse(text);
  } catch {
    return undefined;
  }
}

export const api = {
  teams: () => request<Team[]>("GET", "/teams"),
  team: (id: number) => request<Team>("GET", `/teams/${id}`),
  createTeam: (name: string, members: string[]) => request<Team>("POST", "/teams", { name, members }),

  retrospectives: (teamId: number) => request<Retrospective[]>("GET", `/teams/${teamId}/retrospectives`),
  retrospective: (id: number) => request<Retrospective>("GET", `/retrospectives/${id}`),
  createRetrospective: (teamId: number, title: string, date?: string) =>
    request<Retrospective>("POST", `/teams/${teamId}/retrospectives`, { title, date: date || undefined }),
  closeRetrospective: (id: number) => request<Retrospective>("PUT", `/retrospectives/${id}/close`),

  feedbackItems: (retroId: number) => request<FeedbackItem[]>("GET", `/retrospectives/${retroId}/feedback-items`),
  addFeedbackItem: (retroId: number, content: string, type: "WENT_WELL" | "NEEDS_IMPROVEMENT", author: string) =>
    request<FeedbackItem>("POST", `/retrospectives/${retroId}/feedback-items`, { content, type, author }),
  updateFeedbackItem: (id: number, content: string) => request<FeedbackItem>("PUT", `/feedback-items/${id}`, { content }),
  deleteFeedbackItem: (id: number) => request<void>("DELETE", `/feedback-items/${id}`),

  addActionItem: (retroId: number, content: string, priority: Priority, author: string) =>
    request<ActionItem>("POST", `/retrospectives/${retroId}/action-items`, { content, priority, author }),
  updateActionItem: (id: number, content: string, priority: Priority) =>
    request<ActionItem>("PUT", `/action-items/${id}`, { content, priority }),
  completeActionItem: (id: number) => request<ActionItem>("PUT", `/action-items/${id}/complete`),
  teamActionItems: (teamId: number, filter: ActionItemFilter) => {
    const query = new URLSearchParams();
    if (filter.priority) query.set("priority", filter.priority);
    if (filter.completed !== undefined) query.set("completed", String(filter.completed));
    const suffix = query.size > 0 ? `?${query}` : "";
    return request<ActionItem[]>("GET", `/teams/${teamId}/action-items${suffix}`);
  },

  summarise: (retroId: number) => request<Summary>("POST", `/retrospectives/${retroId}/summary`),
};

import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import type { FeedbackItem, Retrospective, Team } from "../../api/types";
import { mockApi, renderApp } from "../../test/render";
import { setViewport } from "../../test/setup";

const TEAM: Team = { id: 3, name: "Payments squad", members: ["Mira Okafor", "Tomas Brandt"] };
const OPEN: Retrospective = { id: 12, teamId: 3, title: "Sprint 42", date: "2026-10-20", status: "OPEN" };
const CLOSED: Retrospective = { ...OPEN, status: "CLOSED" };
const ITEMS: FeedbackItem[] = [
  { id: 1, retrospectiveId: 12, type: "WENT_WELL", content: "Pairing on the refund flow", author: "Mira Okafor" },
  { id: 2, retrospectiveId: 12, type: "NEEDS_IMPROVEMENT", content: "CI takes 25 minutes", author: "Tomas Brandt" },
  { id: 3, retrospectiveId: 12, type: "ACTION_ITEM", content: "Split the CI pipeline", author: "Mira Okafor", priority: "HIGH", completed: false },
  { id: 4, retrospectiveId: 12, type: "ACTION_ITEM", content: "Update the runbook", author: "Tomas Brandt", priority: "LOW", completed: true },
];

function apiFor(retro: Retrospective, extra: Record<string, unknown> = {}) {
  return mockApi({
    "GET /api/teams": { body: [TEAM] },
    "GET /api/teams/3": { body: TEAM },
    "GET /api/teams/3/retrospectives": { body: [retro] },
    "GET /api/retrospectives/12": { body: retro },
    "GET /api/retrospectives/12/feedback-items": { body: ITEMS },
    ...(extra as Record<string, { status?: number; body?: unknown }>),
  });
}

describe("retrospective board", () => {
  it("lets the team add feedback while the retro is open", async () => {
    // Arrange
    setViewport(true);
    const calls = apiFor(OPEN, {
      "POST /api/retrospectives/12/feedback-items": { status: 201, body: { ...ITEMS[0], id: 9, content: "Quick reviews" } },
    });
    renderApp("/retrospectives/12");
    const user = userEvent.setup();

    // Act
    await user.type(await screen.findByRole("textbox", { name: "Add to Went well" }), "Quick reviews");
    await user.click(within(screen.getByRole("region", { name: /Went well/ })).getByRole("button", { name: "Add" }));

    // Assert: posted as the first member, the default "Posting as"
    await waitFor(() =>
      expect(calls).toContainEqual({ method: "POST", path: "/api/retrospectives/12/feedback-items", body: { content: "Quick reviews", type: "WENT_WELL", author: "Mira Okafor" } }),
    );
    expect(screen.getByRole("button", { name: "Close retrospective" })).toBeInTheDocument();
  });

  it("freezes a closed retro: no composers, no edit or delete, a banner and the summary instead", async () => {
    // Arrange
    setViewport(true);
    apiFor(CLOSED);

    // Act
    renderApp("/retrospectives/12");

    // Assert
    expect(await screen.findByText("This retrospective is closed")).toBeInTheDocument();
    expect(await screen.findByText("Pairing on the refund flow")).toBeInTheDocument();
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Edit" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Delete" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Close retrospective" })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Summarize" })).toBeEnabled();
    // Decision F6: an open action item can still be completed after the retro is closed.
    expect(screen.getByRole("button", { name: "Mark as done: Split the CI pipeline" })).toBeInTheDocument();
  });

  it("shows a done action item as done, without a control to undo it", async () => {
    // Arrange
    setViewport(true);
    apiFor(OPEN);

    // Act
    renderApp("/retrospectives/12");

    // Assert
    expect(await screen.findByText("Update the runbook")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Update the runbook/ })).not.toBeInTheDocument();
    expect(screen.getAllByText("Done").length).toBeGreaterThan(0);
  });

  it("asks before marking an action item as done, and says it can't be undone", async () => {
    // Arrange
    setViewport(true);
    const calls = apiFor(OPEN, { "PUT /api/action-items/3/complete": { body: { ...ITEMS[2], completed: true } } });
    renderApp("/retrospectives/12");
    const user = userEvent.setup();

    // Act
    await user.click(await screen.findByRole("button", { name: "Mark as done: Split the CI pipeline" }));
    const dialog = screen.getByRole("dialog", { name: "Mark this action item as done?" });
    expect(dialog).toHaveTextContent("doesn't allow reopening");
    await user.click(within(dialog).getByRole("button", { name: "Mark as done" }));

    // Assert
    await waitFor(() => expect(calls.some((call) => call.method === "PUT" && call.path === "/api/action-items/3/complete")).toBe(true));
  });

  it("shows the server's reason when the summary isn't configured", async () => {
    // Arrange
    setViewport(true);
    apiFor(CLOSED, {
      "POST /api/retrospectives/12/summary": {
        status: 503,
        body: { error: "Retrospective 12: summaries are not available, the model is not configured", status: 503, timestamp: "t" },
      },
    });
    renderApp("/retrospectives/12");
    const user = userEvent.setup();

    // Act
    await user.click(await screen.findByRole("button", { name: "Summarize" }));

    // Assert
    expect(await screen.findByText("Summaries aren't set up on this server")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Summarize" })).toBeDisabled();
  });

  it("turns the three columns into tabs on a small screen", async () => {
    // Arrange
    setViewport(false);
    apiFor(OPEN);
    renderApp("/retrospectives/12");
    const user = userEvent.setup();

    // Act
    await user.click(await screen.findByRole("tab", { name: /Actions/ }));

    // Assert
    expect(screen.getByRole("tab", { name: /Actions/ })).toHaveAttribute("aria-selected", "true");
    expect(screen.getByRole("tabpanel")).toHaveTextContent("Split the CI pipeline");
    expect(screen.queryByText("Pairing on the refund flow")).not.toBeInTheDocument();
  });
});

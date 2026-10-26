import { describe, expect, it } from "vitest";
import { mockApi } from "../test/render";
import { api } from "./client";
import { ApiError, NETWORK_ERROR, reasonOf } from "./errors";

describe("reasonOf", () => {
  it.each([
    ["Team new: a team named 'Alpha' already exists", "A team named 'Alpha' already exists"],
    ["Retrospective 4: already closed", "Already closed"],
    ["ActionItem 15: already completed, cannot be undone", "Already completed, cannot be undone"],
    ["Request -: Not Found", "Not Found"],
    ["no prefix here", "No prefix here"],
  ])("turns %j into %j", (message, expected) => {
    // Act & Assert
    expect(reasonOf(message)).toBe(expected);
  });
});

describe("api client", () => {
  it("throws an ApiError with the reason and the status for an error body", async () => {
    // Arrange
    mockApi({ "PUT /api/retrospectives/4/close": { status: 409, body: { error: "Retrospective 4: already closed", status: 409, timestamp: "t" } } });

    // Act
    const failure = await api.closeRetrospective(4).catch((error: unknown) => error);

    // Assert
    expect(failure).toBeInstanceOf(ApiError);
    expect(failure).toMatchObject({ status: 409, message: "Already closed", serverMessage: "Retrospective 4: already closed" });
  });

  it("reads Retry-After from a rate-limited summary", async () => {
    // Arrange
    mockApi({
      "POST /api/retrospectives/7/summary": {
        status: 503,
        body: { error: "Retrospective 7: the summary service is busy, try again in a minute", status: 503, timestamp: "t" },
        headers: { "Retry-After": "60" },
      },
    });

    // Act
    const failure = await api.summarise(7).catch((error: unknown) => error);

    // Assert
    expect(failure).toMatchObject({ status: 503, retryAfter: 60 });
  });

  it("explains an unreachable server in plain language", async () => {
    // Arrange: no handler, so fetch rejects
    mockApi({});

    // Act
    const failure = await api.teams().catch((error: unknown) => error);

    // Assert
    expect(failure).toMatchObject({ status: 0, message: NETWORK_ERROR });
  });

  it("sends the action item filters as query parameters and nothing when they are empty", async () => {
    // Arrange
    const calls = mockApi({
      "GET /api/teams/3/action-items?priority=HIGH&completed=false": { body: [] },
      "GET /api/teams/3/action-items": { body: [] },
    });

    // Act
    await api.teamActionItems(3, { priority: "HIGH", completed: false });
    await api.teamActionItems(3, {});

    // Assert
    expect(calls.map((call) => call.path)).toEqual(["/api/teams/3/action-items?priority=HIGH&completed=false", "/api/teams/3/action-items"]);
  });

  it("returns nothing for 204 No Content", async () => {
    // Arrange
    mockApi({ "DELETE /api/feedback-items/9": { status: 204 } });

    // Act & Assert
    await expect(api.deleteFeedbackItem(9)).resolves.toBeUndefined();
  });
});

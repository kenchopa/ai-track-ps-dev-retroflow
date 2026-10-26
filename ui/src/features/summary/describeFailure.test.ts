import { describe, expect, it } from "vitest";
import { ApiError } from "../../api/errors";
import { describeFailure } from "./SummaryPanel";

const failure = (status: number, message: string, retryAfter?: number) => new ApiError(status, `Retrospective 7: ${message}`, retryAfter);

describe("describeFailure", () => {
  it.each([
    [failure(503, "the summary service is busy, try again in a minute", 60), "warning", "The summary service is busy", true],
    [failure(503, "summaries are not available, the model is not configured"), "info", "Summaries aren't set up on this server", false],
    [failure(504, "the summary took too long, try again later"), "warning", "The summary took too long", true],
    [failure(502, "the summary service gave an answer we could not use, try again"), "warning", "The summary couldn't be used", true],
    [failure(400, "has 2000 feedback items, a summary takes at most 500"), "danger", "No summary for this retrospective", false],
    [new Error("offline"), "danger", "RetroFlow can't be reached", true],
  ])("maps %s to a %s alert", (error, tone, title, canRetry) => {
    // Act
    const described = describeFailure(error);

    // Assert
    expect(described).toMatchObject({ tone, title, canRetry });
  });

  it("tells the user how long to wait when rate limited", () => {
    // Act & Assert
    expect(describeFailure(failure(503, "busy", 45)).detail).toContain("45 seconds");
  });
});

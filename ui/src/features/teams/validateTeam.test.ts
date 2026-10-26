import { describe, expect, it } from "vitest";
import { validateTeam } from "./CreateTeamDialog";

describe("validateTeam", () => {
  it("accepts a name and at least one member", () => {
    // Act & Assert
    expect(validateTeam("Payments squad", ["Mira Okafor"])).toEqual({});
  });

  it("asks for a name and a member when both are missing", () => {
    // Act
    const errors = validateTeam("   ", []);

    // Assert
    expect(errors).toEqual({ name: "Give the team a name.", members: "Add at least one member." });
  });

  it("rejects a name over 100 characters, like the API", () => {
    // Arrange
    const tooLong = "x".repeat(101);

    // Act & Assert
    expect(validateTeam(tooLong, ["Mira"]).name).toBe("Keep the name under 100 characters.");
  });
});

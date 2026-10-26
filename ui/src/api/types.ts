// Mirrors the Spring Boot DTOs in src/main/java/com/stackcraft/retroflow/dto.

export type Team = {
  id: number;
  name: string;
  members: string[];
};

export type RetroStatus = "OPEN" | "CLOSED";

export type Retrospective = {
  id: number;
  teamId: number;
  title: string;
  /** ISO date, e.g. "2026-10-26". */
  date: string;
  status: RetroStatus;
};

export type FeedbackType = "WENT_WELL" | "NEEDS_IMPROVEMENT" | "ACTION_ITEM";
export type Priority = "LOW" | "MEDIUM" | "HIGH";

export const PRIORITIES: Priority[] = ["HIGH", "MEDIUM", "LOW"];

/** A feedback item; `priority` and `completed` are only present for action items. */
export type FeedbackItem = {
  id: number;
  retrospectiveId: number;
  type: FeedbackType;
  content: string;
  author: string;
  priority?: Priority;
  completed?: boolean;
};

export type ActionItem = FeedbackItem & { type: "ACTION_ITEM"; priority: Priority; completed: boolean };

export type Summary = {
  wentWell: string[];
  needsImprovement: string[];
  openActionItems: { id: number; summary: string; priority: Priority }[];
};

/** The error body every endpoint returns (StackCraft standard 2.2). */
export type ApiErrorBody = {
  error: string;
  status: number;
  timestamp: string;
};

export type ActionItemFilter = {
  priority?: Priority;
  completed?: boolean;
};

export function isActionItem(item: FeedbackItem): item is ActionItem {
  return item.type === "ACTION_ITEM";
}

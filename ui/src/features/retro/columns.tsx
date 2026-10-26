import { ListChecks, ThumbsUp, Wrench } from "@phosphor-icons/react";
import type { ReactNode } from "react";
import type { FeedbackType } from "../../api/types";

export type Column = {
  type: FeedbackType;
  title: string;
  /** Tab label on small screens, where three long labels don't fit side by side. */
  short: string;
  /** Prompt in the composer: a question, so people know what to write. */
  prompt: string;
  emptyOpen: string;
  emptyClosed: string;
  icon: ReactNode;
  /** Category marker colour; always shown next to the icon and the title. */
  marker: string;
};

export const COLUMNS: Column[] = [
  {
    type: "WENT_WELL",
    title: "Went well",
    short: "Went well",
    prompt: "What went well this sprint?",
    emptyOpen: "Nothing yet. Start with something the team should keep doing.",
    emptyClosed: "Nobody added anything here.",
    icon: <ThumbsUp />,
    marker: "bg-positive-soft text-positive",
  },
  {
    type: "NEEDS_IMPROVEMENT",
    title: "Needs improvement",
    short: "To improve",
    prompt: "What slowed the team down?",
    emptyOpen: "Nothing yet. What would make the next sprint easier?",
    emptyClosed: "Nobody added anything here.",
    icon: <Wrench />,
    marker: "bg-attention-soft text-attention",
  },
  {
    type: "ACTION_ITEM",
    title: "Action items",
    short: "Actions",
    prompt: "What will the team do about it?",
    emptyOpen: "No action items yet. Turn an improvement into something concrete someone will do.",
    emptyClosed: "No action items were agreed.",
    icon: <ListChecks />,
    marker: "bg-accent-soft text-accent",
  },
];

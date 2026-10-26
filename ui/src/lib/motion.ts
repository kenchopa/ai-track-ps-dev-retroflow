import type { Transition, Variants } from "framer-motion";

/** Layout moves and list items (design-engineering spring, DESIGN.md). */
export const spring: Transition = { type: "spring", stiffness: 100, damping: 20, mass: 1 };

/** Small, quick feedback (press, toggle). */
export const snappy: Transition = { type: "spring", stiffness: 400, damping: 30 };

/** Parent of a list that reveals its children one after another (40 ms apart, at most 8 staggered). */
export const listVariants: Variants = {
  hidden: {},
  show: { transition: { staggerChildren: 0.04 } },
};

export const itemVariants: Variants = {
  hidden: { opacity: 0, y: 6 },
  show: { opacity: 1, y: 0, transition: spring },
  exit: { opacity: 0, scale: 0.98, transition: { duration: 0.15 } },
};

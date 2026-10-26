import { motion, useReducedMotion } from "framer-motion";
import { memo } from "react";
import { cx } from "../../lib/cx";

/**
 * The only perpetual motion in RetroFlow: an open retrospective "breathes".
 * Memoized and isolated so the loop never re-renders its parent; static under reduced motion.
 */
export const StatusDot = memo(function StatusDot({ open, className }: { open: boolean; className?: string }) {
  const reduce = useReducedMotion();
  const dot = cx("inline-block h-2 w-2 shrink-0 rounded-full", open ? "bg-accent" : "bg-fg-subtle", className);
  if (!open || reduce) return <span aria-hidden className={dot} />;
  return (
    <motion.span
      aria-hidden
      className={dot}
      animate={{ opacity: [1, 0.35, 1] }}
      transition={{ duration: 2.4, repeat: Infinity, ease: "easeInOut" }}
    />
  );
});

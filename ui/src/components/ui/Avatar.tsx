import { cx } from "../../lib/cx";
import { initials, tintFor } from "../../lib/people";

type Props = {
  name: string;
  size?: "sm" | "md";
  /** When the name is already written next to the avatar, hide it from screen readers. */
  decorative?: boolean;
  className?: string;
};

export function Avatar({ name, size = "sm", decorative = false, className }: Props) {
  return (
    <span
      role={decorative ? undefined : "img"}
      aria-label={decorative ? undefined : name}
      aria-hidden={decorative || undefined}
      title={name}
      className={cx(
        "inline-flex shrink-0 items-center justify-center rounded-full font-semibold select-none",
        size === "sm" ? "h-6 w-6 text-[10px]" : "h-8 w-8 text-xs",
        tintFor(name),
        className,
      )}
    >
      {initials(name)}
    </span>
  );
}

/** The first few members as overlapping avatars, the rest as "+n". */
export function AvatarStack({ names, max = 4 }: { names: string[]; max?: number }) {
  const shown = names.slice(0, max);
  const rest = names.length - shown.length;
  return (
    <span className="inline-flex items-center" aria-label={`${names.length} members: ${names.join(", ")}`} role="img">
      {shown.map((name) => (
        <Avatar key={name} name={name} decorative className="-ml-1 ring-2 ring-canvas first:ml-0" />
      ))}
      {rest > 0 && (
        <span aria-hidden className="-ml-1 inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-surface-2 px-1.5 font-mono text-[10px] text-fg-muted ring-2 ring-canvas tabular">
          +{rest}
        </span>
      )}
    </span>
  );
}

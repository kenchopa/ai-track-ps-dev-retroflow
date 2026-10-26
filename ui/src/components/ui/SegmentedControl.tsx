import { useId, type ReactNode } from "react";
import { cx } from "../../lib/cx";

export type Segment<T extends string> = { value: T; label: ReactNode; count?: number };

type Props<T extends string> = {
  /** Read by screen readers as the group name. */
  label: string;
  value: T;
  options: Segment<T>[];
  onChange: (value: T) => void;
  size?: "sm" | "md";
  className?: string;
};

/** A row of native radio buttons: arrow keys, focus and form semantics come for free. */
export function SegmentedControl<T extends string>({ label, value, options, onChange, size = "sm", className }: Props<T>) {
  const name = useId();
  return (
    <fieldset className={cx("min-w-0", className)}>
      <legend className="sr-only">{label}</legend>
      <div className="inline-flex max-w-full flex-wrap gap-0.5 rounded-md border border-line bg-surface-2 p-0.5">
        {options.map((option) => (
          <label key={option.value} className="relative">
            <input
              type="radio"
              name={name}
              value={option.value}
              checked={option.value === value}
              onChange={() => onChange(option.value)}
              className="peer sr-only"
            />
            <span
              className={cx(
                "inline-flex cursor-pointer items-center gap-1.5 rounded-[7px] px-3 text-sm whitespace-nowrap text-fg-muted transition-[background-color,color,box-shadow] duration-150 ease-out-expo",
                "hover:text-fg peer-checked:bg-surface peer-checked:font-medium peer-checked:text-fg peer-checked:shadow-sm",
                "peer-focus-visible:outline-2 peer-focus-visible:outline-offset-1 peer-focus-visible:outline-accent",
                size === "sm" ? "h-7" : "h-9",
              )}
            >
              {option.label}
              {option.count !== undefined && <span className="font-mono text-xs text-fg-subtle tabular">{option.count}</span>}
            </span>
          </label>
        ))}
      </div>
    </fieldset>
  );
}

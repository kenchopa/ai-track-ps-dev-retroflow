import { useId, type ComponentProps, type ReactNode } from "react";
import { CaretDown } from "@phosphor-icons/react";
import { cx } from "../../lib/cx";

const control =
  "focus-ring w-full rounded-md border border-control bg-surface text-sm text-fg placeholder:text-fg-subtle " +
  "transition-colors duration-150 hover:border-fg-muted aria-[invalid=true]:border-danger disabled:cursor-not-allowed disabled:opacity-60";

type ShellProps = {
  id: string;
  label: string;
  /** Hide the label visually but keep it for screen readers (for compact inline composers). */
  hideLabel?: boolean;
  optional?: boolean;
  helper?: ReactNode;
  error?: string;
  aside?: ReactNode;
  children: ReactNode;
};

/** Label above, helper or error below, `gap-2` (design-engineering rule 6). */
export function FieldShell({ id, label, hideLabel, optional, helper, error, aside, children }: ShellProps) {
  return (
    <div className="flex flex-col gap-2">
      <div className={cx("flex items-baseline justify-between gap-3", hideLabel && "sr-only")}>
        <label htmlFor={id} className="text-sm font-medium text-fg">
          {label}
          {optional && <span className="ml-1 font-normal text-fg-subtle">(optional)</span>}
        </label>
        {aside}
      </div>
      {children}
      {error ? (
        <p id={`${id}-error`} className="text-xs text-danger">
          {error}
        </p>
      ) : (
        helper && (
          <p id={`${id}-helper`} className="text-xs text-fg-subtle">
            {helper}
          </p>
        )
      )}
    </div>
  );
}

function describedBy(id: string, error?: string, helper?: ReactNode) {
  if (error) return `${id}-error`;
  return helper ? `${id}-helper` : undefined;
}

type CommonProps = { label: string; hideLabel?: boolean; optional?: boolean; helper?: ReactNode; error?: string };

export function TextField({ label, hideLabel, optional, helper, error, className, id: givenId, ...rest }: CommonProps & ComponentProps<"input">) {
  const generated = useId();
  const id = givenId ?? generated;
  return (
    <FieldShell id={id} label={label} hideLabel={hideLabel} optional={optional} helper={helper} error={error}>
      <input
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(id, error, helper)}
        className={cx(control, "h-10 px-3", className)}
        {...rest}
      />
    </FieldShell>
  );
}

export function TextArea({
  label,
  hideLabel,
  optional,
  helper,
  error,
  className,
  maxLength,
  value,
  id: givenId,
  ...rest
}: CommonProps & ComponentProps<"textarea">) {
  const generated = useId();
  const id = givenId ?? generated;
  const length = typeof value === "string" ? value.length : 0;
  const showCount = maxLength !== undefined && length > maxLength * 0.8;
  return (
    <FieldShell
      id={id}
      label={label}
      hideLabel={hideLabel}
      optional={optional}
      helper={helper}
      error={error}
      aside={showCount && <span className={cx("font-mono text-xs tabular", length >= maxLength ? "text-danger" : "text-fg-subtle")}>{`${length}/${maxLength}`}</span>}
    >
      <textarea
        id={id}
        value={value}
        maxLength={maxLength}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(id, error, helper)}
        className={cx(control, "min-h-[5.5rem] resize-y px-3 py-2.5 leading-relaxed [field-sizing:content]", className)}
        {...rest}
      />
    </FieldShell>
  );
}

export function SelectField({ label, hideLabel, optional, helper, error, className, children, id: givenId, ...rest }: CommonProps & ComponentProps<"select">) {
  const generated = useId();
  const id = givenId ?? generated;
  return (
    <FieldShell id={id} label={label} hideLabel={hideLabel} optional={optional} helper={helper} error={error}>
      <div className="relative">
        <select
          id={id}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy(id, error, helper)}
          className={cx(control, "h-10 cursor-pointer appearance-none pr-9 pl-3", className)}
          {...rest}
        >
          {children}
        </select>
        <CaretDown aria-hidden size={16} className="pointer-events-none absolute top-1/2 right-3 -translate-y-1/2 text-fg-subtle" />
      </div>
    </FieldShell>
  );
}

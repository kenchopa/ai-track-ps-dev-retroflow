import type { ButtonHTMLAttributes, ReactNode } from "react";
import { cx } from "../../lib/cx";

type Variant = "primary" | "secondary" | "ghost" | "danger";
type Size = "sm" | "md";

const base =
  "focus-ring relative inline-flex select-none items-center justify-center gap-2 overflow-hidden whitespace-nowrap rounded-md font-medium " +
  "transition-[background-color,border-color,color,transform,opacity] duration-150 ease-out-expo active:scale-[0.98] " +
  "disabled:cursor-not-allowed disabled:opacity-50 disabled:active:scale-100";

const sizes: Record<Size, string> = {
  sm: "h-8 px-3 text-sm",
  md: "h-10 px-4 text-sm",
};

const variants: Record<Variant, string> = {
  primary: "bg-accent text-on-accent shadow-sm hover:bg-accent-strong",
  secondary: "border border-control bg-surface text-fg hover:bg-surface-2",
  ghost: "text-fg-muted hover:bg-surface-2 hover:text-fg",
  danger: "bg-danger text-on-danger shadow-sm hover:opacity-90",
};

export type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant;
  size?: Size;
  /** Keeps the label (no layout jump), disables the button and shows a progress line. */
  loading?: boolean;
  icon?: ReactNode;
};

export function Button({ variant = "secondary", size = "md", loading = false, icon, className, children, disabled, type = "button", ...rest }: ButtonProps) {
  return (
    <button
      type={type}
      className={cx(base, sizes[size], variants[variant], className)}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      {...rest}
    >
      {icon}
      {children}
      {loading && <span aria-hidden className="skeleton-shimmer absolute inset-x-0 bottom-0 h-0.5 bg-current opacity-40" />}
    </button>
  );
}

export type IconButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  /** Read by screen readers and shown as a tooltip. */
  label: string;
  size?: Size;
  tone?: "default" | "danger";
};

export function IconButton({ label, size = "sm", tone = "default", className, children, type = "button", ...rest }: IconButtonProps) {
  return (
    <button
      type={type}
      aria-label={label}
      title={label}
      className={cx(
        "focus-ring inline-flex shrink-0 items-center justify-center rounded-md text-fg-subtle transition-[background-color,color,transform] duration-150 ease-out-expo active:scale-[0.94] disabled:cursor-not-allowed disabled:opacity-40",
        size === "sm" ? "h-8 w-8" : "h-10 w-10",
        tone === "danger" ? "hover:bg-danger-soft hover:text-danger" : "hover:bg-surface-2 hover:text-fg",
        className,
      )}
      {...rest}
    >
      {children}
    </button>
  );
}

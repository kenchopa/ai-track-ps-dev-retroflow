import { useId, useRef, type KeyboardEvent, type ReactNode } from "react";
import { NavLink } from "react-router";
import { cx } from "../../lib/cx";

export type Tab<T extends string> = { value: T; label: ReactNode; count?: number };

type TabsProps<T extends string> = {
  label: string;
  value: T;
  tabs: Tab<T>[];
  onChange: (value: T) => void;
  /** Id prefix for the panels: render each panel with `id={panelId(value)}` and `aria-labelledby={tabId(value)}`. */
  idPrefix: string;
  /** Give every tab the same width (for a few short tabs on a small screen). */
  fill?: boolean;
  className?: string;
};

export const tabId = (prefix: string, value: string) => `${prefix}-tab-${value}`;
export const panelId = (prefix: string, value: string) => `${prefix}-panel-${value}`;

const tabClass = (active: boolean) =>
  cx(
    "focus-ring relative -mb-px inline-flex h-10 items-center gap-2 border-b-2 px-1 text-sm whitespace-nowrap transition-colors duration-150",
    active ? "border-accent font-medium text-fg" : "border-transparent text-fg-muted hover:text-fg",
  );

/** In-page tabs (WAI-ARIA tabs pattern): arrow keys move between tabs, one tab stop. */
export function Tabs<T extends string>({ label, value, tabs, onChange, idPrefix, fill = false, className }: TabsProps<T>) {
  const refs = useRef(new Map<T, HTMLButtonElement>());

  function onKeyDown(event: KeyboardEvent, index: number) {
    const step = event.key === "ArrowRight" ? 1 : event.key === "ArrowLeft" ? -1 : 0;
    if (step === 0) return;
    event.preventDefault();
    const next = tabs[(index + step + tabs.length) % tabs.length];
    if (!next) return;
    onChange(next.value);
    refs.current.get(next.value)?.focus();
  }

  return (
    <div role="tablist" aria-label={label} className={cx("flex overflow-x-auto border-b border-line", fill ? "gap-0" : "gap-5", className)}>
      {tabs.map((tab, index) => {
        const active = tab.value === value;
        return (
          <button
            key={tab.value}
            ref={(element) => {
              if (element) refs.current.set(tab.value, element);
            }}
            type="button"
            role="tab"
            id={tabId(idPrefix, tab.value)}
            aria-selected={active}
            aria-controls={panelId(idPrefix, tab.value)}
            tabIndex={active ? 0 : -1}
            onClick={() => onChange(tab.value)}
            onKeyDown={(event) => onKeyDown(event, index)}
            className={cx(tabClass(active), fill && "flex-1 justify-center")}
          >
            {tab.label}
            {tab.count !== undefined && <span className="font-mono text-xs text-fg-subtle tabular">{tab.count}</span>}
          </button>
        );
      })}
    </div>
  );
}

/** Route tabs: each tab is its own URL, so they are links in a nav, not ARIA tabs. */
export function TabLinks({ label, links }: { label: string; links: { to: string; label: ReactNode; end?: boolean }[] }) {
  const id = useId();
  return (
    <nav aria-labelledby={id} className="flex gap-5 overflow-x-auto border-b border-line">
      <span id={id} className="sr-only">
        {label}
      </span>
      {links.map((link) => (
        <NavLink key={link.to} to={link.to} end={link.end} className={({ isActive }) => tabClass(isActive)}>
          {link.label}
        </NavLink>
      ))}
    </nav>
  );
}

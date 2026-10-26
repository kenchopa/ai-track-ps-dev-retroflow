import { X } from "@phosphor-icons/react";
import { useEffect, useId, useRef, useState, type FormEvent, type ReactNode } from "react";
import { cx } from "../../lib/cx";
import { Button, IconButton } from "./Button";
import { InlineAlert } from "./Feedback";

const CLOSE_MS = 220;

type DialogProps = {
  open: boolean;
  onClose: () => void;
  title: string;
  description?: ReactNode;
  children?: ReactNode;
  footer?: ReactNode;
  /** Render the body and footer inside a form; Enter submits. */
  onSubmit?: (event: FormEvent<HTMLFormElement>) => void;
  width?: "sm" | "md";
};

/**
 * A native modal <dialog>: focus trap, Escape and the backdrop come from the browser.
 * Opening and closing are animated in CSS (styles.css, `.rf-dialog`).
 */
export function Dialog({ open, onClose, title, description, children, footer, onSubmit, width = "sm" }: DialogProps) {
  const ref = useRef<HTMLDialogElement>(null);
  const titleId = useId();
  const descriptionId = useId();
  // Keep the content for the 200 ms close animation, then drop it: closed dialogs render nothing.
  const [closing, setClosing] = useState(false);
  const wasOpen = useRef(open);

  useEffect(() => {
    if (wasOpen.current && !open) {
      setClosing(true);
      const timer = window.setTimeout(() => setClosing(false), CLOSE_MS);
      wasOpen.current = open;
      return () => window.clearTimeout(timer);
    }
    wasOpen.current = open;
    return undefined;
  }, [open]);
  const rendered = open || closing;

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);

  const body = (
    <>
      <div className="flex items-start justify-between gap-4 px-6 pt-5">
        <div className="flex flex-col gap-1">
          <h2 id={titleId} className="text-lg font-semibold tracking-tight text-fg">
            {title}
          </h2>
          {description && (
            <div id={descriptionId} className="max-w-[60ch] text-sm leading-relaxed text-fg-muted">
              {description}
            </div>
          )}
        </div>
        <IconButton label="Close" onClick={onClose} className="-mt-1 -mr-2">
          <X />
        </IconButton>
      </div>
      {children && <div className="flex flex-col gap-4 px-6 pt-5">{children}</div>}
      {footer && <div className="flex flex-col-reverse gap-2 px-6 pt-6 pb-5 sm:flex-row sm:justify-end">{footer}</div>}
    </>
  );

  return (
    <dialog
      ref={ref}
      aria-labelledby={titleId}
      aria-describedby={description ? descriptionId : undefined}
      onClose={onClose}
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
      onClick={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
      className={cx(
        "rf-dialog m-auto w-[calc(100vw-2rem)] rounded-xl border border-line bg-surface p-0 text-fg shadow-lg",
        width === "sm" ? "max-w-md" : "max-w-xl",
      )}
    >
      {rendered &&
        (onSubmit ? (
          <form
            noValidate
            onSubmit={(event) => {
              event.preventDefault();
              onSubmit(event);
            }}
          >
            {body}
          </form>
        ) : (
          body
        ))}
    </dialog>
  );
}

type ConfirmProps = {
  open: boolean;
  onClose: () => void;
  title: string;
  description: ReactNode;
  confirmLabel: string;
  tone?: "danger" | "accent";
  onConfirm: () => Promise<unknown>;
};

/** For destructive or one-way actions: says what will happen, then asks. Shows the server's reason if it fails. */
export function ConfirmDialog({ open, onClose, title, description, confirmLabel, tone = "danger", onConfirm }: ConfirmProps) {
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (open) setError(null);
  }, [open]);

  async function confirm() {
    setPending(true);
    setError(null);
    try {
      await onConfirm();
      onClose();
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : "Something went wrong. Try again.");
    } finally {
      setPending(false);
    }
  }

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={title}
      description={description}
      footer={
        <>
          <Button onClick={onClose} disabled={pending}>
            Cancel
          </Button>
          <Button variant={tone === "danger" ? "danger" : "primary"} loading={pending} onClick={confirm} autoFocus>
            {confirmLabel}
          </Button>
        </>
      }
    >
      {error && <InlineAlert tone="danger">{error}</InlineAlert>}
    </Dialog>
  );
}

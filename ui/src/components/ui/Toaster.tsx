import { CheckCircle, WarningOctagon } from "@phosphor-icons/react";
import { AnimatePresence, motion } from "framer-motion";
import { createContext, useCallback, useContext, useMemo, useRef, useState, type ReactNode } from "react";
import { cx } from "../../lib/cx";
import { spring } from "../../lib/motion";

type Toast = { id: number; message: string; tone: "success" | "danger" };
type ToastApi = { notify: (message: string, tone?: Toast["tone"]) => void };

const ToastContext = createContext<ToastApi>({ notify: () => undefined });

export function useToast() {
  return useContext(ToastContext);
}

const DURATION_MS = 4000;

/** Short confirmations after an action ("Retrospective closed"). Announced politely, gone after 4 s. */
export function Toaster({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const next = useRef(1);

  const notify = useCallback((message: string, tone: Toast["tone"] = "success") => {
    const id = next.current++;
    setToasts((current) => [...current.slice(-2), { id, message, tone }]);
    window.setTimeout(() => setToasts((current) => current.filter((toast) => toast.id !== id)), DURATION_MS);
  }, []);

  const api = useMemo(() => ({ notify }), [notify]);

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div aria-live="polite" className="pointer-events-none fixed inset-x-4 bottom-4 z-40 flex flex-col items-end gap-2 sm:inset-x-auto sm:right-6 sm:bottom-6">
        <AnimatePresence initial={false}>
          {toasts.map((toast) => (
            <motion.div
              key={toast.id}
              layout
              initial={{ opacity: 0, y: 8, scale: 0.98 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, scale: 0.98 }}
              transition={spring}
              className={cx(
                "pointer-events-auto flex max-w-sm items-center gap-2.5 rounded-lg border border-line bg-surface px-4 py-3 text-sm text-fg shadow-md",
              )}
            >
              {toast.tone === "success" ? <CheckCircle aria-hidden className="text-positive" /> : <WarningOctagon aria-hidden className="text-danger" />}
              {toast.message}
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </ToastContext.Provider>
  );
}

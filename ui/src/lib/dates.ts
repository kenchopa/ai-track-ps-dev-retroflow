const LONG = new Intl.DateTimeFormat("en-GB", { day: "numeric", month: "short", year: "numeric" });

/** "2026-10-26" becomes "26 Oct 2026", read as a calendar date (no time zone shift). */
export function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.split("-").map(Number);
  if (!year || !month || !day) return isoDate;
  return LONG.format(new Date(year, month - 1, day));
}

/** Today as "YYYY-MM-DD" in the user's time zone, for date inputs. */
export function todayIso(): string {
  const now = new Date();
  const pad = (value: number) => String(value).padStart(2, "0");
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

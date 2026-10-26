/**
 * An API failure with the server's message in plain language. The API words every error as
 * "[EntityType] [identifier]: [reason]" (StackCraft 2.1); people only need the reason.
 */
export class ApiError extends Error {
  readonly status: number;
  /** The server's full message, e.g. "Retrospective 4: already closed". */
  readonly serverMessage: string;
  /** Seconds to wait before retrying, from the Retry-After header (summary rate limit). */
  readonly retryAfter?: number;

  constructor(status: number, serverMessage: string, retryAfter?: number) {
    super(reasonOf(serverMessage));
    this.name = "ApiError";
    this.status = status;
    this.serverMessage = serverMessage;
    this.retryAfter = retryAfter;
  }
}

/** "Team new: a team named 'Alpha' already exists" becomes "A team named 'Alpha' already exists". */
export function reasonOf(message: string): string {
  const reason = message.replace(/^[A-Z][A-Za-z]* \S+: /, "").trim();
  return reason.length === 0 ? message : reason.charAt(0).toUpperCase() + reason.slice(1);
}

export const NETWORK_ERROR = "RetroFlow can't be reached. Check that the server is running and try again.";

/** Any thrown value as one sentence for the screen. */
export function messageOf(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  return NETWORK_ERROR;
}

export function isNotFound(error: unknown): boolean {
  return error instanceof ApiError && error.status === 404;
}

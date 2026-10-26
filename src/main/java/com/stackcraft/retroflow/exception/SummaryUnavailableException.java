package com.stackcraft.retroflow.exception;

/**
 * The language model behind the retrospective summary failed, or gave an
 * answer we can't trust. The message tells the caller what happened in
 * StackCraft's format (standard 2.1) and never contains provider details,
 * so it is safe to return as is (standard 4.2).
 */
public class SummaryUnavailableException extends RetroflowException {

    /** What went wrong; the global exception handler maps each reason to an HTTP status. */
    public enum Reason {
        NOT_CONFIGURED("summaries are not available, the model is not configured"),
        TIMEOUT("the summary took too long, try again later"),
        RATE_LIMITED("the summary service is busy, try again in a minute"),
        PROVIDER_ERROR("the summary service failed, try again later"),
        INVALID_ANSWER("the summary service gave an answer we could not use, try again");

        private final String reason;

        Reason(String reason) {
            this.reason = reason;
        }
    }

    private final Reason reason;

    public SummaryUnavailableException(Long retrospectiveId, Reason reason) {
        super("Retrospective " + retrospectiveId + ": " + reason.reason);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

}

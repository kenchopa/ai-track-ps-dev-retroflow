package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.exception.SummaryUnavailableException;

import java.util.List;

/**
 * A language model that summarises a retrospective. The service layer only
 * knows this interface; the provider (OpenAI today) lives in the {@code ai}
 * package, and tests replace it with a stub so they never call a real API.
 */
public interface SummaryModel {

    /**
     * Asks the model for a summary as JSON that follows the summary schema.
     * The answer is not trusted: {@link SummaryService} validates it.
     *
     * @param input the retrospective's feedback, as data
     * @return the raw JSON answer and the tokens it cost
     * @throws SummaryUnavailableException if the model is not configured, times out,
     *                                     is rate limited, fails or refuses to answer
     */
    Answer summarise(Input input);

    /** What the model gets: only ids, types, texts and action item state. No authors. */
    record Input(Long retrospectiveId, String title, List<Item> items) {
    }

    /** One feedback item; {@code priority} and {@code completed} are null unless it is an action item. */
    record Item(Long id, String type, String content, String priority, Boolean completed) {
    }

    /** The model's raw answer and its token usage, for cost tracking. */
    record Answer(String json, long inputTokens, long outputTokens) {
    }

}

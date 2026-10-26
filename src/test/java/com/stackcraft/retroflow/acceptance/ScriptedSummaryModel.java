package com.stackcraft.retroflow.acceptance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stackcraft.retroflow.exception.SummaryUnavailableException;
import com.stackcraft.retroflow.service.SummaryModel;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stands in for OpenAI. A scenario says how the model behaves ("writes a faithful summary",
 * "times out", ...); the real validation in SummaryValidator still checks every answer.
 */
public class ScriptedSummaryModel implements SummaryModel {

    /** How the model answers in the current scenario. */
    public enum Behaviour {
        FAITHFUL, LEAVES_OUT_IMPROVEMENTS, NOT_JSON,
        TIMES_OUT, RATE_LIMITED, FAILS, NOT_CONFIGURED
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    private volatile Behaviour behaviour = Behaviour.FAITHFUL;
    private final AtomicInteger calls = new AtomicInteger();

    void reset() {
        behaviour = Behaviour.FAITHFUL;
        calls.set(0);
    }

    void behave(Behaviour next) {
        behaviour = next;
    }

    int calls() {
        return calls.get();
    }

    @Override
    public Answer summarise(Input input) {
        calls.incrementAndGet();
        return switch (behaviour) {
            case FAITHFUL -> new Answer(summaryOf(input, true), 0, 0);
            case LEAVES_OUT_IMPROVEMENTS -> new Answer(summaryOf(input, false), 0, 0);
            case NOT_JSON -> new Answer("Sure! Here is your summary: the team did great.", 0, 0);
            case TIMES_OUT -> throw failure(input, SummaryUnavailableException.Reason.TIMEOUT);
            case RATE_LIMITED -> throw failure(input, SummaryUnavailableException.Reason.RATE_LIMITED);
            case FAILS -> throw failure(input, SummaryUnavailableException.Reason.PROVIDER_ERROR);
            case NOT_CONFIGURED -> throw failure(input, SummaryUnavailableException.Reason.NOT_CONFIGURED);
        };
    }

    /** One point per item, word for word, and every open action item: what a faithful model returns. */
    private static String summaryOf(Input input, boolean withImprovements) {
        ObjectNode summary = JSON.createObjectNode();
        ArrayNode wentWell = summary.putArray("wentWell");
        ArrayNode improvements = summary.putArray("needsImprovement");
        ArrayNode actions = summary.putArray("openActionItems");
        for (Item item : input.items()) {
            switch (item.type()) {
                case "WENT_WELL" -> wentWell.add(item.content());
                case "NEEDS_IMPROVEMENT" -> {
                    if (withImprovements) {
                        improvements.add(item.content());
                    }
                }
                default -> {
                    if (!Boolean.TRUE.equals(item.completed())) {
                        actions.addObject().put("id", item.id()).put("summary", item.content()).put("priority", item.priority());
                    }
                }
            }
        }
        return summary.toString();
    }

    private static SummaryUnavailableException failure(Input input, SummaryUnavailableException.Reason reason) {
        return new SummaryUnavailableException(input.retrospectiveId(), reason);
    }
}

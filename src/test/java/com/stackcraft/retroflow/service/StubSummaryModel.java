package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.exception.SummaryUnavailableException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A {@link SummaryModel} for tests: it answers what the test tells it to and
 * remembers what it was asked. No test ever calls a real model API.
 */
class StubSummaryModel implements SummaryModel {

    static final long INPUT_TOKENS = 120;
    static final long OUTPUT_TOKENS = 40;

    private Function<Input, Answer> behaviour = input -> {
        throw new AssertionError("the test did not say what the model answers");
    };
    private final List<Input> calls = new ArrayList<>();

    /** The model answers with JSON built from what it was asked. */
    void answers(Function<Input, String> json) {
        behaviour = input -> new Answer(json.apply(input), INPUT_TOKENS, OUTPUT_TOKENS);
    }

    void fails(SummaryUnavailableException.Reason reason) {
        behaviour = input -> {
            throw new SummaryUnavailableException(input.retrospectiveId(), reason);
        };
    }

    List<Input> calls() {
        return calls;
    }

    void reset() {
        calls.clear();
    }

    @Override
    public Answer summarise(Input input) {
        calls.add(input);
        return behaviour.apply(input);
    }

    /** Replaces the OpenAI model in the Spring context of a test. */
    @TestConfiguration
    static class Config {

        @Bean
        @Primary
        StubSummaryModel stubSummaryModel() {
            return new StubSummaryModel();
        }

    }

}
